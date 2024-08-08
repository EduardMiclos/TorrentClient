import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Operations {
	
	public static String calculateHash(String rawString) throws IOException {
        byte[] hashData = rawString.getBytes();
		byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(hashData);
            String checksum = new BigInteger(1, hash).toString(16);
		return checksum;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
	}

    public static String calculateHash(byte[] bytesArr) throws IOException {
        byte[] hashData = bytesArr;
		byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(hashData);
            String checksum = new BigInteger(1, hash).toString(16);
		return checksum;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
	}

}

