import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtil {
  public static String ReadFileContent(String fileName) {
    // Create the file handler for the file.
    File file = new File(fileName);
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    DataInputStream dis = null;
    int fileLength = Integer.MAX_VALUE;
    if (file.length() < Integer.MAX_VALUE) {
      fileLength = (int)file.length();
    }
    byte[] textBytes = new byte[fileLength];

    try {
      fis = new FileInputStream(file);
      bis = new BufferedInputStream(fis);
      dis = new DataInputStream(bis);

      dis.read(textBytes);

      fis.close();
      bis.close();
      dis.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new String(textBytes);
  }
}