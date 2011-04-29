import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class StreamingFileUtil {
	private File file;
	private FileInputStream fis = null;
	private BufferedInputStream bis = null;
	private DataInputStream dis = null;
	
	public StreamingFileUtil(String filename) {
		file = new File(filename);
		try {
	        fis = new FileInputStream(file);
	        bis = new BufferedInputStream(fis);
	        dis = new DataInputStream(bis);
	    } catch (IOException e) {
	    	fis = null;
	    	bis = null;
	    	dis = null;
			e.printStackTrace();
		}
	}
	
	public String getNextLine() {
		String line = null;
		try {
			line = dis.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}
	
	public void close() {
		try {
			fis.close();
			bis.close();
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}