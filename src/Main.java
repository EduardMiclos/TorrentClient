import java.io.IOException;

public class Main {
	
	public static void main(String[] args) throws IOException {
		
		TorrentTrackerClient trackerClient = new TorrentTrackerClient("tracker.openbittorrent.com", 80);
		byte[] response = trackerClient.connect();
		
		for (byte b : response) {			
			System.out.println(b & 0xFF);
		}
		
//		socket = new DatagramSocket();
//		
//		try {
//			address = InetAddress.getByName("tracker.openbittorrent.com");
//		} catch (UnknownHostException e) {
//			System.out.print("Fatal error: Unknown host. Aborting!");
//			System.exit(1);
//		}
//		
//		
//		port = 80;
//		
//		byte[] connectionId = HexFormat.of().parseHex("0000041727101980");
//		byte[] action = HexFormat.of().parseHex("00000000");
//		byte[] transactionId = HexFormat.of().parseHex(Integer.toHexString((new Random()).nextInt()));
//		
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		outputStream.write(connectionId);
//		outputStream.write(action);
//		outputStream.write(transactionId);
//		
//		byte[] socketMessage = outputStream.toByteArray();
//		
//		
//		DatagramPacket packet = new DatagramPacket(socketMessage, socketMessage.length, address, port);
//		socket.send(packet);
//		packet = new DatagramPacket(socketMessage, socketMessage.length);
//		socket.receive(packet);
//		
//		String receivedMessage = new String(packet.getData(), 0, packet.getLength());
//		socket.close();
		
		
		
	}

}
