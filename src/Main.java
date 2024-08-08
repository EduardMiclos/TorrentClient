import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
	
public class Main {
	
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		BittorrentClient torrentClient = new BittorrentClient();
//		torrentClient.removeTorrent("input/input.torrent");
//		torrentClient.addTorrent("input/input.torrent");
		
//		 ArrayList<Map.Entry<String, Integer>> trackers = torrentClient.getTorrentSessions().get(0).getTorrentFile().getTrackers();
//		 System.out.println(trackers.get(10));
		// torrentClient.removeTorrent("input/input.torrent");
        
		torrentClient.download("input/input.torrent");
	}

}

