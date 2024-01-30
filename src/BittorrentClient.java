import java.io.IOException;
import java.security.SecureRandom;

public class BittorrentClient {
	private TorrentFileHandler torrentFile;
	private byte[] peerId;

	private enum BittorentState {
		IDLE, DOWNLOADING, PAUSED, COMPLETED
	}

	private BittorentState state;

	private long downloadedBytes;
	private long leftBytes;
	private long uploadedBytes;

	private static int maxPeersNumber = 5;

	public BittorrentClient(String torrentFile) {
		this.torrentFile = new TorrentFileHandler(torrentFile);
		this.peerId = generatePeerId();
		this.state = BittorentState.IDLE;

		this.downloadedBytes = 0;
		this.leftBytes = this.torrentFile.getDownloadSize();
		this.uploadedBytes = 0;
	}

	private byte[] generatePeerId() {
		byte[] randomBytes = new byte[20];
		SecureRandom randomBytesGenerator = new SecureRandom();
		randomBytesGenerator.nextBytes(randomBytes);

		return randomBytes;
	}

	private void startDownload() throws IOException {
		/**
		 * In the future, here I should use the trackers extracted from the torrent
		 * file.
		 */
		TrackerHandler trackerHandler = new TrackerHandler("tracker.openbittorrent.com", 6969);
		trackerHandler.announce(torrentFile.getHash(), peerId, downloadedBytes, leftBytes, uploadedBytes,
				maxPeersNumber);

	}

	private void pauseDownload() {

	}

	private void resumeDownload() {

	}

	public void download() throws IOException {
		switch (state) {
		case IDLE: {
			System.out.println("INFO: Starting download...");
			startDownload();
			return;
		}
		case DOWNLOADING: {
			System.out.println("INFO: Already started downloading!");
			return;
		}
		case COMPLETED: {
			System.out.println("INFO: The download is already completed!");
			return;
		}
		case PAUSED: {
			System.out.println("INFO: Resuming download...");
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
}
