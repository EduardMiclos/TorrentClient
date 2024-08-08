import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bencode.Bencode;
import bencode.Type;
import java.net.URI;
import java.net.URISyntaxException;

public class TorrentFileHandler implements Serializable {
	private static final long serialVersionUID = 7855109060053759549L;

	/** Relative path to the original torrent file. */
	private String path;

	/**
	 * A list with all the trackers and their corresponding addresses + port numbers.
	 */
	private ArrayList<Map.Entry<String, Integer>> trackers;

	/** The hash of the whole torrent file. */
	private String hash;
	
	/** The 20 bytes long info hash. */
	private byte[] infoHash;

	/** The size of one piece of the downloadable content, in Bytes. */
	private long pieceSize;

	/** The download size, in Bytes, of all the files. */
	private long downloadSize;

	public TorrentFileHandler(String path) throws NoSuchAlgorithmException, IOException {
		this.path = path;
		this.trackers = new ArrayList<Map.Entry<String, Integer>>();
		this.pieceSize = 0;
		this.downloadSize = 0;

		this.hash = "123124124"; //Operations.calculateHash(Files.readAllBytes(Paths.get(this.path)));
		
		parse();
	}
	
	private Map.Entry<String, Integer> extractHostnameAndPort(String tracker) {
		Pattern pattern = Pattern.compile("(:\\d+)");
		Matcher matcher = pattern.matcher(tracker);
		
		while (matcher.find()) {
			String portString = matcher.group(1);
			int portNumber = Integer.parseInt(portString.substring(1));
			String uriString = tracker.replace(portString, "");
			
			try {
	            URI uri = new URI(uriString);
	            String hostname = uri.getHost();
	            return new AbstractMap.SimpleEntry<>(hostname, portNumber);
	        } catch (URISyntaxException e) {
	            System.err.println("Invalid URI: " + uriString);
	            e.printStackTrace();
	            
	            return null;
	        }
		}

		return null;
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
			ArrayList<Object> trackers = (ArrayList<Object>) torrentDict.get("announce-list");
			
			for (Object tracker : trackers) {
				String trackerString = ((ArrayList<?>) tracker).get(0).toString();
				this.trackers.add(extractHostnameAndPort(trackerString));
			}

			/**
			 * Extracting the length (size in Bytes) of each file and summing up the lengths
			 * inside the downloadSize variable.
			 */
			LinkedHashMap<String, Object> torrentDictInfo = (LinkedHashMap<String, Object>)torrentDict.get("info");
			ArrayList<Object> filesInfo = (ArrayList<Object>) torrentDictInfo.get("files");
			for (Object file : filesInfo) {
				LinkedHashMap<String, Object> fileInfo = (LinkedHashMap<String, Object>) file;
				this.downloadSize += (long) fileInfo.get("length");
			}

			/** Converting the torrent info to string & calculating SHA1 hash. */
			setInfoHash(torrentDictInfo.toString());			

			/**
			 * Extracting the piece length and storing into inside the pieceSize variable.
			 */
			LinkedHashMap<String, Object> fileInfo = (LinkedHashMap<String, Object>) torrentDict.get("info");
			this.pieceSize = (long) fileInfo.get("piece length");

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

	public ArrayList<Map.Entry<String, Integer>> getTrackers() {
		return trackers;
	}

	public void setTrackers(ArrayList<Map.Entry<String, Integer>> trackers) {
		this.trackers = trackers;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
	
	public byte[] getInfoHash() {
		return infoHash;
	}

	public void setInfoHash(String torrentInfoString) {
		try {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			crypt.reset();
			crypt.update(torrentInfoString.getBytes("UTF-8"));
			byte[] sha1 = crypt.digest();

			this.infoHash = sha1;
		}
		catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch(UnsupportedEncodingException e) {
			e.printStackTrace();
		}
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
