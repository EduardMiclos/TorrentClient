import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bencode.Bencode;
import bencode.Type;

public class TorrentFileHandler {
	/** Relative path to the original torrent file. */
	private String path;

	/** A list with all the trackers and their corresponding addresses + port numbers. */
	private ArrayList<String> trackers;
	
	/** The 20 bytes-long info hash of the torrent file. */
	private byte[] hash;
	
	/** The size of one piece of the downloadable content, in Bytes. */
	private long pieceSize;
	
	/** The download size, in Bytes, of all the files. */
	private long downloadSize;
	
	public TorrentFileHandler(String path) {
		this.path = path;
		this.trackers = new ArrayList<String>();
		this.hash = new byte[20];
		this.pieceSize = 0;
		this.downloadSize = 0;
		
		parse();
	}
	
	private void parse() {
		FileInputStream fileObj;
		
		try {
			fileObj = new FileInputStream(this.path);
			byte[] torrentFileContent = fileObj.readAllBytes();
			fileObj.close();
			
			Bencode bencode = new Bencode();
			Map<String, Object> torrentDict = bencode.decode(torrentFileContent, Type.DICTIONARY);
			
			/** Extracting all the trackers into a list of strings. */
			ArrayList <Object> trackers = (ArrayList<Object>)torrentDict.get("announce-list");
			for (Object tracker : trackers) {
				this.trackers.add(((ArrayList<?>)tracker).get(0).toString());
			}
			
			/** Extracting the length (size in Bytes) of each file and summing up the lengths inside the downloadSize variable. */
			ArrayList <Object> filesInfo = (ArrayList<Object>)((LinkedHashMap<String, Object>)torrentDict.get("info")).get("files");
			for (Object file : filesInfo) {
				LinkedHashMap<String, Object> fileInfo = (LinkedHashMap<String, Object>)file;
				this.downloadSize +=  (long)fileInfo.get("length");
			}
			
			/** Extracting the piece length and storing into inside the pieceSize variable. */
			LinkedHashMap<String, Object> fileInfo = (LinkedHashMap<String, Object>)torrentDict.get("info");
			this.pieceSize = (long)fileInfo.get("piece length");
			
		} catch (FileNotFoundException e) {
			System.err.println("Error: Torrent file could not be found.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error: Unable to read parse and read the torrent file.");
			System.exit(1);
		}
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ArrayList<String> getTrackers() {
		return trackers;
	}

	public void setTrackers(ArrayList<String> trackers) {
		this.trackers = trackers;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public long getPieceSize() {
		return pieceSize;
	}

	public void setPieceSize(long pieceSize) {
		this.pieceSize = pieceSize;
	}

	public long getDownloadSize() {
		return downloadSize;
	}

	public void setDownloadSize(long downloadSize) {
		this.downloadSize = downloadSize;
	}
}
