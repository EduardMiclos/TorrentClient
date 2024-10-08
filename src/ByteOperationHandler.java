import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteOperationHandler {

	/**
	 * Creates a ByteBuffer of capacity <capacity>, encapsulates the byte array and
	 * returns the ByteBuffer.
	 */
	private static ByteBuffer wrapByteArrayInBuffer(byte[] byteArr, int capacity) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(capacity);
		byteBuffer.put(byteArr);
		byteBuffer.rewind();
		return byteBuffer;
	}

	public static byte[] readByteArrayFromFile(String filePath) throws IOException {
		File fileObject = new File(filePath);

		if (!fileObject.exists())
			return null;

		FileInputStream inputStream = new FileInputStream(filePath);
		byte[] byteArr = inputStream.readAllBytes();
		inputStream.close();

		return byteArr;
	}

	public static void writeByteArrayToFile(byte[] byteArr, String filePath) throws IOException {
		File fileObject = new File(filePath);

		if (!fileObject.getParentFile().exists())
			fileObject.getParentFile().mkdirs();
		
		if (fileObject.exists())
			fileObject.delete();

		fileObject.createNewFile();

		FileOutputStream outputStream = new FileOutputStream(filePath);
		outputStream.write(byteArr, 0, byteArr.length);
		outputStream.close();
	}

	public static byte[] mergeByteArrays(byte[]... byteArrays) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		for (byte[] byteArray : byteArrays) {
			outputStream.write(byteArray);
		}

		return outputStream.toByteArray();
	}

	public static byte[] readWord(byte[] byteArr) {
		return readWord(byteArr, 0);
	}

	public static byte[] readWord(byte[] byteArr, int from) {
		return Arrays.copyOfRange(byteArr, from, from + 4);
	}

	public static byte[] readBytes(byte[] byteArr, int from, int to) {
		return Arrays.copyOfRange(byteArr, from, to);
	}

	public static byte[] int16ToByteArray(short n) {
		return ByteBuffer.allocate(2).putShort(n).array();
	}

	public static byte[] int32ToByteArray(int n) {
		return ByteBuffer.allocate(4).putInt(n).array();
	}

	public static byte[] int64ToByteArray(long n) {
		return ByteBuffer.allocate(8).putLong(n).array();
	}

	public static short byteArrayToInt16(byte[] word) {
		return wrapByteArrayInBuffer(word, 16).getShort();
	}

	public static int byteArrayToInt32(byte[] word) {
		return wrapByteArrayInBuffer(word, 32).getInt();
	}

	public static long byteArrayToInt64(byte[] word) {
		return wrapByteArrayInBuffer(word, 64).getLong();
	}

	public static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i=0; i < b.length; i++) {
		  result +=
				Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	  }

	public static byte[] reverseByteOrder(byte[] byteArr) {
		if (byteArr == null)
			return null;

		int i = 0;
		int j = byteArr.length - 1;
		byte tmp;
		while (j > i) {
			tmp = byteArr[i];
			byteArr[i] = byteArr[j];
			byteArr[j] = tmp;
			i++;
			j--;
		}

		return byteArr;
	}
}
