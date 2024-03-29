import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;

/**
 * This class is responsible for handling the connection with the tracker and
 * extract useful information to be used for downloading the file pieces.
 */
public class TrackerHandler {
	private DatagramSocket socket;
	private InetAddress trackerAddress;
	private int trackerPort;

	/** Is true if the client has connected in the current session. */
	private boolean isConnected = false;

	/** 8 bytes */
	private static byte[] initialConnectionId = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x17,
			(byte) 0x27, (byte) 0x10, (byte) 0x19, (byte) 0x80 };

	/** 4 bytes */
	private static byte[] initialTransactionId = new byte[] { (byte) 0x13, (byte) 0x07, (byte) 0x20, (byte) 0x01 };

	/** 4 bytes */
	private static byte[] clientKey = new byte[] { (byte) 0x99, (byte) 0x14, (byte) 0x20, (byte) 0x11 };

	/** 8 bytes */
	private byte[] currentConnectionId = null;

	/** 4 bytes */
	private byte[] currentTransactionId = null;

	/** All possible actions when it comes to interacting with the Tracker. */
	private static class TrackerAction {

		/**
		 * Initial connection. We're retrieving a connection id that we're going to be
		 * using when further communication is exchanged with the tracker.
		 */
		/** 4 bytes */
		private static byte[] connect = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		private static byte connectFlag = 0;

		/**
		 * Retrieving peers IP Addresses based on the torrent hash. We're going to
		 * connect to them and download our file piece by piece.
		 */
		/** 4 bytes */
		private static byte[] announce = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
		private static byte announceFlag = 1;

		/**
		 * Checking general torrent information: - the current number of connected
		 * seeds. - the number of times the torrent has been downloaded. - the current
		 * number of connected leechers.
		 */
		/** 4 bytes */
		private static byte[] scrape = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02 };
		private static byte scrapeFlag = 2;

	}

	/** Tracker events refer to the current status of the downloading process. */
	private static class TrackerEvent {

		private static byte[] none = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

		/** The client completed the download of the entire torrent. */
		private static byte[] completed = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

		/** The client starts downloading the torrent. */
		private static byte[] started = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02 };

		/** The client stops participating in the torrent. */
		private static byte[] stopped = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03 };

	}

	private static final int[] listeningPortRange = { 6881, 6889 };
	private int listeningPort;

	private static final String CONNECTION_INFO_CACHEFILE_PATH = "cache/connectionInfo";
	private static final String DOWNLOAD_INFO_CACHEFILE_PATH = "cache/downloadInfo";

	public TrackerHandler(String trackerAddress, int trackerPort) {
		try {
			this.trackerAddress = InetAddress.getByName(trackerAddress);
		} catch (UnknownHostException e) {
			System.err.println("Fatal error: Unknown host. Aborting!");
			System.exit(1);
		}

		this.trackerPort = trackerPort;

		for (int port = listeningPortRange[0]; port <= listeningPortRange[1]; port++) {
			try {
				this.socket = new DatagramSocket(port);
				this.listeningPort = port;
			} catch (SocketException e) {
				System.err.println(String.format("Failed: Unable to open socket on port %d.", port));
				System.err.println(e.getMessage());

				if (port >= listeningPortRange[1]) {
					System.err.println("Error: No Bittorent port is open. Aborting!");
					System.exit(1);
				}
			}
		}

	}

	public boolean hasConnected() throws IOException {
		if (isConnected)
			return true;

		byte[] connectionInfo = ByteOperationHandler.readByteArrayFromFile(CONNECTION_INFO_CACHEFILE_PATH);
		if (connectionInfo == null || connectionInfo.length != 16)
			return false;

		byte[] word = ByteOperationHandler.readWord(connectionInfo);
		long serverPacketType = ByteOperationHandler.byteArrayToInt64(word);

		word = ByteOperationHandler.readWord(connectionInfo, 4);
		if (!Arrays.equals(initialTransactionId, word))
			return false;

		currentConnectionId = ByteOperationHandler.readBytes(connectionInfo, 8, 16);
		return serverPacketType == TrackerAction.connectFlag;
	}

	/** Sending 16 bytes */
	/** Receiving 16 bytes */
	public byte[] connect() throws IOException {
		byte[] connectionId = initialConnectionId;
		byte[] action = TrackerAction.connect;
		byte[] transactionId = initialTransactionId;
		byte[] socketMessage = ByteOperationHandler.mergeByteArrays(connectionId, action, transactionId);

		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, this.trackerAddress,
				this.trackerPort);
		socket.send(packet);

		packet = new DatagramPacket(new byte[16], 0, 16);
		socket.receive(packet);

		ByteOperationHandler.writeByteArrayToFile(packet.getData(), CONNECTION_INFO_CACHEFILE_PATH);

		currentConnectionId = ByteOperationHandler.readBytes(packet.getData(), 8, 16);
		isConnected = true;

		return packet.getData();
	}

	/** Sending 100 bytes */
	/**
	 * Receiving 20 bytes + n*6 bytes, where n varies if NumWant is -1 else n =
	 * numWant.
	 */
	public byte[] announce(byte[] torrentHash, byte[] peerId, long downloadedBytes, long leftBytes, long uploadedBytes,
			int maxPeersNumber) throws IOException {
		if (!hasConnected())
			connect();

		byte[] connectionId = currentConnectionId;
		byte[] action = TrackerAction.announce;
		byte[] transactionId = initialTransactionId;
		byte[] downloaded = ByteOperationHandler.int64ToByteArray(downloadedBytes);
		byte[] left = ByteOperationHandler.int64ToByteArray(leftBytes);
		byte[] uploaded = ByteOperationHandler.int64ToByteArray(uploadedBytes);
		byte[] event = TrackerEvent.none;

		if (downloadedBytes == 0)
			event = TrackerEvent.started;
		else if (leftBytes == 0)
			event = TrackerEvent.completed;

		byte[] ip = ByteOperationHandler.int32ToByteArray(0);
		byte[] key = clientKey;
		byte[] numWant = ByteOperationHandler.int32ToByteArray(maxPeersNumber);

		/** Important note: Ports reserved for BitTorrent are typically 6881-6889. */
		byte[] port = ByteOperationHandler.int16ToByteArray((short) socket.getPort());

		byte[] socketMessage = ByteOperationHandler.mergeByteArrays(connectionId, action, transactionId, torrentHash,
				peerId, downloaded, left, uploaded, event, ip, key, numWant, port);

		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, trackerAddress, trackerPort);
		socket.send(packet);

		int packetLength = 20 + maxPeersNumber * 6;
		packet = new DatagramPacket(new byte[packetLength], packetLength);
		socket.receive(packet);

		return packet.getData();

	}
}
