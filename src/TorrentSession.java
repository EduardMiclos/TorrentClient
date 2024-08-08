import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class TorrentSession implements Serializable {
	private static final long serialVersionUID = 2790628950977085119L;
	
	/** We need this handler in order to parse the torrent file and extract useful information. */
	private TorrentFileHandler torrentFile;
	private byte[] peerId;
	
	private static final int peerIdLengthInBytes = 20;
	private static final int trackerHandlerIdLengthInBytes = 20;

	private enum TorrentState {
		IDLE, 
		DOWNLOADING, 
		PAUSED, 
		COMPLETED
	}
	private TorrentState state;

	private long downloadedBytes;
	private long leftBytes;
	private long uploadedBytes;

	/** Maximum number of peer-to-peer connections at one time. */
	private static int maxPeersNumber = 5;

	private String cachePath;
	
	public TorrentSession(String cachePath, String torrentFile) throws NoSuchAlgorithmException, IOException {
		this.torrentFile = new TorrentFileHandler(torrentFile);
		this.peerId = generatePeerId();
		this.state = TorrentState.IDLE;

		this.downloadedBytes = 0;
		this.leftBytes = this.torrentFile.getDownloadSize();
		this.uploadedBytes = 0;
		
		this.cachePath = cachePath + '/' + this.torrentFile.getHash() + '/';
	}
	
	public TorrentSession(String cachePath, TorrentFileHandler torrentFileHandler) throws NoSuchAlgorithmException, IOException {
		this.torrentFile = torrentFileHandler;
		this.peerId = generatePeerId();
		this.state = TorrentState.IDLE;
		
		this.downloadedBytes = 0;
		this.leftBytes = this.torrentFile.getDownloadSize();
		this.uploadedBytes = 0;
		
		this.cachePath = cachePath + '/' + this.torrentFile.getHash() + '/';
	}

	private byte[] generatePeerId() {
		byte[] randomBytes = new byte[peerIdLengthInBytes];
		SecureRandom randomBytesGenerator = new SecureRandom();
		randomBytesGenerator.nextBytes(randomBytes);

		return randomBytes;
	}
	
	/** Each trackerHandler has one unique ID in order to create unique
	 *  cache locations for each connection to that specific tracker and store
	 *  torrent information + peers IPs. I don't know yet if each tracker returns the
	 *  same torrent information or not, so this will stay for now.
	 */
	private String generateTrackerHandlerId() {
		StringBuilder uidBuilder = new StringBuilder();
		int maxlen = trackerHandlerIdLengthInBytes;
		
		while (maxlen > 0) {
			int randomInt = 97 + new Random().nextInt(25);
			
			uidBuilder.append((char)randomInt);
			maxlen--;
		}
		
		return uidBuilder.toString();
	}
	
	private boolean isUniqueTrackerHandlerId(String trackerHandlerId) {
		/** TBD. */
		return true;
	}
	
	private Map.Entry<String, Integer> extractRandomTracker() {
		if (torrentFile.getTrackers().isEmpty()) return null;
		
		int rndIndex = new Random().nextInt(torrentFile.getTrackers().size());
		return torrentFile.getTrackers().get(rndIndex);
	}
	
	private void startDownload() throws IOException {
		/** Setting up the current state to DOWNLOADING. */
		state = TorrentState.DOWNLOADING;
		
		Map.Entry<String, Integer> tracker = extractRandomTracker();
		
		/** If the torrent has no trackers, the download can't happen. */
		if (tracker == null) return;
		
		String trackerHandlerId = "123124124_tracker"; //Operations.calculateHash(tracker.getKey() + tracker.getValue());
		String trackerCachePath = cachePath + "/" + trackerHandlerId;
		
		// TrackerHandler trackerHandler = new TrackerHandler(trackerCachePath, tracker.getKey(), tracker.getValue());



		// temporary!!!!
		TrackerHandler trackerHandler = new TrackerHandler(trackerCachePath, "tracker.dler.org", 6969);
		// temporary!!!!



		// TrackerHandlerRunnerThread trackerHandlerThread = new TrackerHandlerRunnerThread(trackerHandler);
		// trackerHandlerThread.start();
		
		
//		 trackerHandler.connect();
		trackerHandler.announce(torrentFile.getInfoHash(), peerId, downloadedBytes, leftBytes, uploadedBytes, maxPeersNumber);
		
		// resumeDownload() ???
	}

	private void pauseDownload() {
		state = TorrentState.PAUSED;
	}

	private void resumeDownload() {
		
	}

	public void download() throws IOException {
		switch (state) {
		case IDLE: {
			System.out.println("INFO: Attempting to start download...");
			startDownload();
			return; 
		}
		case DOWNLOADING: {
			// System.out.println("INFO: Already started downloading!");
			startDownload();
			return;
		}
		case COMPLETED: {
			System.out.println("INFO: The download is already downloaded!");
			return;
		}
		case PAUSED: {
			System.out.println("INFO: Attempting to resume download...");
			resumeDownload();
			return;
		}
		default:
			throw new IllegalArgumentException("Unexpected state: " + state);
		}
	}

	public void pause() {
		switch (state) {
		case IDLE: {
			System.out.println("INFO: Nothing to pause. No download process on-going!");
			return;
		}
		case DOWNLOADING: {
			System.out.println("INFO: Pausing...");
			pauseDownload();
			return;
		}
		case COMPLETED: {
			System.out.println("INFO: Nothing to pause. The download is completed!");
			return;
		}
		case PAUSED: {
			System.out.println("INFO: Already paused!");
			return;
		}
		default:
			throw new IllegalArgumentException("Unexpected state: " + state);
		}
	}

	public TorrentFileHandler getTorrentFile() {
		return torrentFile;
	}

	public void setTorrentFile(TorrentFileHandler torrentFile) {
		this.torrentFile = torrentFile;
	}
}
