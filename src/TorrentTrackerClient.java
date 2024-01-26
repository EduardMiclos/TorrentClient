import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HexFormat;
import java.util.Random;

/** This class is responsible for handling the connection with the tracker and extract
 *  useful information to be used for downloading the file pieces. */
public class TorrentTrackerClient {
	private DatagramSocket socket;
	private InetAddress trackerAddress;
	private int trackerPort;
	
	private byte[] initialConnectionId = new byte[] { (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x17, 
													  (byte)0x27, (byte)0x10, (byte)0x19, (byte)0x80 };
	
	/** Random value. */
	private byte[] initialTransactionId  = new byte[] { (byte)0x13, (byte)0x07, (byte)0x20, (byte)0x01 };
	
	private byte[] connectAction       = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
	
	private byte[] announceAction      = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01 };
	
	
	private boolean hasConnected = false;
	private byte[] currentConnectionId = null;
	
	public TorrentTrackerClient(String trackerAddress, int trackerPort) throws SocketException {
		try {
			this.trackerAddress = InetAddress.getByName(trackerAddress);
		} catch (UnknownHostException e) {
			System.err.print("Fatal error: Unknown host. Aborting!");
			System.exit(1);
		}
		
		this.trackerPort = trackerPort;
		this.socket = new DatagramSocket();
	}
	
	
	public byte[] connect() throws IOException {
		byte[] connectionId = initialConnectionId;
		byte[] action = connectAction;
		byte[] transactionId = initialTransactionId;
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(connectionId);
		outputStream.write(action);
		outputStream.write(transactionId);
		
		byte[] socketMessage = outputStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, this.trackerAddress, this.trackerPort);
		socket.send(packet);
		
		packet = new DatagramPacket(new byte[16], 0, 16);
		socket.receive(packet);
		
		return packet.getData();
	}
	
	
	public byte[] announce() throws IOException {
		if (!hasConnected)
				connect();
		
		byte[] connectionId = currentConnectionId;
		byte[] action = announceAction;
		byte[] transactionId = initialTransactionId;
		return null;
	}
}
