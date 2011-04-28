import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HdfsFileUtil {
	public static String ReadFileContent(String fileName, Configuration conf) {
		// Open the file and read contents.
		Path path = new Path(fileName);
		FSDataInputStream dis = null;
		int fileSize = 0;
		try {
			FileStatus[] fileStatus = FileSystem.get(conf).listStatus(path);
			fileSize = (int) fileStatus[0].getLen();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		byte[] textBytes = new byte[fileSize];

		try {
			dis = FileSystem.get(conf).open(path);
			dis.read(textBytes);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new String(textBytes);
	}
}