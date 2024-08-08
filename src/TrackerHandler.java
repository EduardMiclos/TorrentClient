import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.DatagramPacket;
/**
 * This class is responsible for handling the connection with the tracker and
 * extract useful information to be used for downloading the file pieces.
 */
public class TrackerHandler implements Serializable {
	private static final long serialVersionUID = 3582081827918830235L;
	
	private DatagramSocket socket;
	private InetAddress trackerAddress;
	private int trackerPort;

	/** 8 bytes */
	private static byte[] initialConnectionId = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x17, (byte) 0x27, (byte) 0x10, (byte) 0x19, (byte) 0x80 };

	/** 4 bytes */
	private byte[] clientKey;

	/** 8 bytes */
	private byte[] lastConnectionId = null;
	
	/** N*5 bytes, N = number of received IP Addresses */
	private byte[] peersIPAddresses = null;

	/** All possible actions when it comes to interacting with the Tracker. */
	private static class TrackerAction {

		/**
		 * Initial connection. We're retrieving a connection id that we're going to be
		 * using when further communication is exchanged with the tracker.
		 */
		private static int connect = 0;

		/**
		 * Retrieving peers IP Addresses based on the torrent hash. We're going to
		 * connect to them and download our file piece by piece.
		 */
		private static int announce = 1;

		/**
		 * Checking general torrent information: 
		 * - the current number of connected seeds.
		 * - the number of times the torrent has been downloaded. 
		 * - the current number of connected leechers.
		 */
		private static int scrape = 2;

	}

	/** Tracker events refer to the current status of the downloading process. */
	private static class TrackerEvent {

		private static int none = 0;

		/** The client completed the download of the entire torrent. */
		private static int completed = 1;

		/** The client starts downloading the torrent. */
		private static int started = 2;

		/** The client stops participating in the torrent. */
		private static int stopped = 3;

	}
	
	

	/** Ports reserved for BitTorrent are typically 6881 - 6889. */
	private static final int[] listeningPortRange = { 6881, 6889 };
	private int listeningPort;

	/** CACHE FILE: connection
	 * 	STORES: last connection ID
	 * 	SIZE: 8 bytes
	 */
	private String CACHEFILE_CONNECTION;
	
	
	/** CACHE FILE: announce
	 * 	STORES: interval, leechers, seeders
	 * 	SIZE: 12 bytes
	 */
	private String CACHEFILE_ANNOUNCE;
	

	/** CACHE FILE: peers
	 * 	STORES: (peer_ip peer_port) x numWant
	 * 	SIZE: 6 x numWant bytes
	 */
	private String CACHEFILE_PEERS;

	public TrackerHandler(String cachePath, String trackerAddress, int trackerPort) {
		try {
			this.trackerAddress = InetAddress.getByName(trackerAddress);
			this.trackerPort = trackerPort;

			this.socket = null;
			this.listeningPort = -1;
			this.clientKey = ByteOperationHandler.int32ToByteArray(new Random().nextInt());

			/** Setting up the cache file names. */
			this.CACHEFILE_CONNECTION = cachePath + "/" + "connection";
			this.CACHEFILE_ANNOUNCE = cachePath + "/" + "announce";
			this.CACHEFILE_PEERS = cachePath + "/" + "peers";
			
		} catch (UnknownHostException e) {
			System.err.println("Fatal error: Unknown host. Aborting!");
			System.exit(1);
		}
	}
	
	private void tryOpenSocket() {
		if (this.socket != null) return;
		
		for (int port = listeningPortRange[0]; port <= listeningPortRange[1]; port++) {
			try {
				this.socket = new DatagramSocket(port);
				this.listeningPort = port;
			
				return;
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
	
	private void closeSocket() {
		if (this.socket != null) {
			this.socket.close();
			this.listeningPort = -1;
		}
	}

	public boolean hasConnectedInThePast() throws IOException {
		if (lastConnectionId != null)
			return true;

		byte[] connectionInfo = ByteOperationHandler.readByteArrayFromFile(CACHEFILE_CONNECTION);
		if (connectionInfo == null || connectionInfo.length != 8)
			return false;

		lastConnectionId = ByteOperationHandler.readBytes(connectionInfo, 0, 4);
		return true;
	}

	
	/** Sending 16 bytes */
	/** Receiving 16 bytes */
	public byte[] connect() throws IOException {
		tryOpenSocket();
		
		byte[] connectionId = initialConnectionId;
		byte[] action = ByteOperationHandler.int32ToByteArray(TrackerAction.connect);
		byte[] transactionId = ByteOperationHandler.int32ToByteArray(new Random().nextInt());
		byte[] socketMessage = ByteOperationHandler.mergeByteArrays(connectionId, action, transactionId);

		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, this.trackerAddress, this.trackerPort);
		socket.send(packet);

		packet = new DatagramPacket(new byte[16], 0, 16);
		socket.receive(packet);
		
		byte[] receivedAction = ByteOperationHandler.readBytes(packet.getData(), 0, 4);
		if (ByteOperationHandler.byteArrayToInt32(receivedAction) != TrackerAction.connect) return null;
		
		byte[] receivedTransactionId = ByteOperationHandler.readBytes(packet.getData(), 4, 8);
		if (!Arrays.equals(transactionId, receivedTransactionId)) return null;
		
		lastConnectionId = ByteOperationHandler.readBytes(packet.getData(), 8, 16);
		ByteOperationHandler.writeByteArrayToFile(lastConnectionId, CACHEFILE_CONNECTION);
		
		return packet.getData();
	}
	
	/** Sending 98 bytes */
	/** Receiving 20 bytes + n*6 bytes, where n varies if NumWant is -1 else n = numWant */
	public byte[] announce(byte[] torrentHash, byte[] peerId, long downloadedBytes, long leftBytes, long uploadedBytes, int maxPeersNumber) throws IOException {
		if (connect() == null) {
			System.err.print("Fatal error: Unable to initialize a connection to the tracker. Aborting!");
			System.exit(1);
		}
		
		byte[] connectionId = lastConnectionId;
		byte[] action = ByteOperationHandler.int32ToByteArray(TrackerAction.announce);
		byte[] transactionId = ByteOperationHandler.int32ToByteArray(new Random().nextInt());
		byte[] downloaded = ByteOperationHandler.int64ToByteArray(downloadedBytes);
		byte[] left = ByteOperationHandler.int64ToByteArray(leftBytes);
		byte[] uploaded = ByteOperationHandler.int64ToByteArray(uploadedBytes);
		byte[] event;
		
		if (downloadedBytes == 0)
			event = ByteOperationHandler.int32ToByteArray(TrackerEvent.started);
		else if (leftBytes == 0)
			event = ByteOperationHandler.int32ToByteArray(TrackerEvent.completed);
		else
			event = ByteOperationHandler.int32ToByteArray(TrackerEvent.none);
		
		byte[] ip = ByteOperationHandler.int32ToByteArray(0);
		byte[] key = clientKey;
		byte[] numWant = ByteOperationHandler.int32ToByteArray(maxPeersNumber);
		byte[] port = ByteOperationHandler.int16ToByteArray((short) socket.getPort());
		
		byte[] socketMessage = ByteOperationHandler.mergeByteArrays(connectionId, action, transactionId, torrentHash,
		peerId, downloaded, left, uploaded, event, ip, key, numWant, port);
		
		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, trackerAddress, trackerPort);
		socket.send(packet);
		
		int packetLength = 20 + maxPeersNumber * 6;
		packet = new DatagramPacket(new byte[packetLength], packetLength);
		
		socket.receive(packet);

		byte[] receivedAction = ByteOperationHandler.readBytes(packet.getData(), 0, 4);
		if (ByteOperationHandler.byteArrayToInt32(receivedAction) != TrackerAction.announce) return null;
		
		byte[] receivedTransactionId = ByteOperationHandler.readBytes(packet.getData(), 4, 8);
		if (!Arrays.equals(transactionId, receivedTransactionId)) return null;

		byte[] interval = ByteOperationHandler.readBytes(packet.getData(), 8, 12);
		byte[] leechers = ByteOperationHandler.readBytes(packet.getData(), 12, 16);
		byte[] seeders = ByteOperationHandler.readBytes(packet.getData(), 16, 20);
		byte[] announceCacheOutput = ByteOperationHandler.mergeByteArrays(interval, leechers, seeders);

		byte[] peersIPAddressesOutput = {};
		for (int i = 0; i < packet.getLength() - 20; i+= 6) {
			byte[] peerIPAddress = ByteOperationHandler.readBytes(packet.getData(), 20 + i, 25 + i);
			peersIPAddressesOutput = ByteOperationHandler.mergeByteArrays(peersIPAddressesOutput, peerIPAddress);
		}

		if (announceCacheOutput.length <= 0) {
			System.out.println("Failed: Unable to announce to the tracker!");
			System.exit(1);
		}
		
		if (peersIPAddressesOutput.length <= 0) {
			System.out.println("Failed: Unable to extract peers IP addresses!");
			System.exit(1);
		}
		
		this.peersIPAddresses = peersIPAddressesOutput;
		ByteOperationHandler.writeByteArrayToFile(announceCacheOutput, CACHEFILE_ANNOUNCE);
		ByteOperationHandler.writeByteArrayToFile(peersIPAddressesOutput, CACHEFILE_PEERS);		

		closeSocket();
		return packet.getData();
	}
}
