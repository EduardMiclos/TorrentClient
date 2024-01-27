import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import bencode.Bencode;
import bencode.Type;

public class BittorrentClient {
	private TorrentFileHandler torrentFile;
	
	public BittorrentClient(String torrentFile) {
		this.torrentFile = new TorrentFileHandler(torrentFile);
	}
	
	private void startDownload() {}
	private void resumeDownload() {}
	
	public void download() {
		
	}
	
	public void pause() {}
}
