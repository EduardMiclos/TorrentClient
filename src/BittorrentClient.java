import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

//import threads.TorrentSessionThread;

public class BittorrentClient {
	private ArrayList<TorrentSession> torrentSessions;
	
	/** The path to the cache folder. */
	private static String CACHEDIR_PATH = "cache";
	
	public BittorrentClient() throws IOException {
		loadUpPreviousSessions();
	}
	 
	/** Some torrents might still be available and 
	 *  not completely downloaded. This function will load
	 *  each torrent session into the torrentSessions arraylist.
	 * @throws IOException 
	 */
	private void loadUpPreviousSessions() throws IOException {
		torrentSessions = new ArrayList<TorrentSession>();
		
		File folder = new File(CACHEDIR_PATH);
		if (!folder.exists()) {
			folder.mkdirs();
			return;
		}
		 
		ObjectInputStream objectInputStream = null;
		FileInputStream fileInputStream = null;
		
		try {	
			for (File dir : folder.listFiles()) {
				
				/** Each directory name is the hash of a torrent file. Inside the
				 *  directory, a torrentSession serialized object is found. */
				if (dir.isDirectory()) {
					fileInputStream = new FileInputStream(dir.getAbsolutePath() + "/torrentsession.ser");
					objectInputStream = new ObjectInputStream(fileInputStream);
					
					TorrentSession torrentSession = (TorrentSession) objectInputStream.readObject();
					torrentSessions.add(torrentSession);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (objectInputStream != null)
				objectInputStream.close();
		}
	}
	
	private boolean saveTorrentSession(TorrentSession torrentSession) {
		String torrentFileHash = torrentSession.getTorrentFile().getHash();
		String torrentFilePath = CACHEDIR_PATH + '/' + torrentFileHash + '/';
		
		File folder = new File(torrentFilePath);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(torrentFilePath + "torrentsession.ser");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			
			objectOutputStream.writeObject(torrentSession);
			objectOutputStream.close();
			
			torrentSessions.add(torrentSession);
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean deleteTorrentSession(TorrentSession torrentSession) throws IOException {
		torrentSessions.remove(torrentSession);

		String torrentFileHash = torrentSession.getTorrentFile().getHash();
		String torrentFilePath = CACHEDIR_PATH + '/' + torrentFileHash + '/';
		
		Path dir = Paths.get(torrentFilePath);
		Files
			.walk(dir)
			.sorted(Comparator.reverseOrder())
			.forEach(path -> {
				try {
					System.out.println("Deleting: " + path);
					Files.delete(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		File folder = new File(torrentFilePath);	
		return folder.delete();
	}
	
	public boolean addTorrent(String torrentFilePath) throws NoSuchAlgorithmException, IOException {
		TorrentFileHandler torrentFileHandler = new TorrentFileHandler(torrentFilePath);
		boolean torrentExists = torrentSessions.stream()
				.filter(torrent -> torrent.getTorrentFile().getHash().equals(torrentFileHandler.getHash()) || torrent.getTorrentFile().getPath().equals(torrentFilePath))
											   .collect(Collectors.toList())
											   .size() != 0;
		
		if (torrentExists) {
			System.err.println("The torrent file " + torrentFilePath + " already exists!");
			return false;
		}
		
		TorrentSession torrentSession = new TorrentSession(CACHEDIR_PATH, torrentFileHandler);
		saveTorrentSession(torrentSession);
		
		System.out.println("Successfully added the torrent file!");
		return true;
	}
	
	public boolean removeTorrent(String torrentFilePath) throws NoSuchAlgorithmException, IOException {
		try {
			TorrentFileHandler torrentFileHandler = new TorrentFileHandler(torrentFilePath);
			TorrentSession torrentSession = torrentSessions.stream()
														   .filter(torrent -> torrent.getTorrentFile().getHash().equals(torrentFileHandler.getHash()) || torrent.getTorrentFile().getPath().equals(torrentFilePath))
														   .collect(Collectors.toList())
														   .get(0);
			
			deleteTorrentSession(torrentSession);

			System.out.println("Successfully deleted the torrent file!");
			return true;
		}
		catch (IndexOutOfBoundsException exception) {
			System.err.println("The torrent file couldn't be found!");
			return false;
		}
	}

	public boolean download(String torrentFilePath) throws IOException {
		try {
			TorrentSession torrentSession = torrentSessions.stream()
														   .filter(torrent -> torrent.getTorrentFile().getPath().equals(torrentFilePath))
														   .collect(Collectors.toList())
														   .get(0);
			
			return download(torrentSession);
		}
		catch (IndexOutOfBoundsException exception) {
			System.err.println("The torrent file couldn't be found!");
			return false;
		}
	}

	public boolean download(TorrentSession torrentSession) throws IOException {
		torrentSession.download();
		saveTorrentSession(torrentSession);

		// TorrentSessionThread thread = new TorrentSessionThread();
		// thread.start();
		// for (int i = 0; i < 100; i++) {
		// 	System.out.println(i);
		// }

		return true;
	}
	
	public boolean pause(String torrentFilePath) throws IOException {
		try {
			TorrentSession torrentSession = torrentSessions.stream()
														   .filter(torrent -> torrent.getTorrentFile().getPath() == torrentFilePath)
														   .collect(Collectors.toList())
														   .get(0);
			
			return pause(torrentSession);
		}
		catch (IndexOutOfBoundsException exception) {
			System.err.println("The torrent file couldn't be found!");
			return false;
		}
	}

	public boolean pause(TorrentSession torrentSession) throws IOException {
		torrentSession.pause();
		saveTorrentSession(torrentSession);
		return true;
	}

	public ArrayList<TorrentSession> getTorrentSessions() {
		return torrentSessions;
	}
}
