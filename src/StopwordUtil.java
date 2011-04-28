import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;

public class StopwordUtil {
	private HashSet<String> stopwordSet;
	
	public StopwordUtil(String fileName, Configuration conf) {
		stopwordSet = new HashSet<String>();
		String stopwordStrings = HdfsFileUtil.ReadFileContent(fileName, conf);
		for (String stopword : stopwordStrings.split("\n")) {
			stopwordSet.add(StringUtil.stem(stopword));
		}
	}

	public StopwordUtil(String fileName) {
		stopwordSet = new HashSet<String>();
		String stopwordStrings = FileUtil.ReadFileContent(fileName);
		for (String stopword : stopwordStrings.split("\n")) {
      String stemmed = StringUtil.stem(stopword);
			stopwordSet.add(stemmed);
		}
    System.out.println(stopwordSet.size());
	}
	
	public boolean isStopWord(String word) {
		return stopwordSet.contains(StringUtil.stem(word));
	}

  public static void main(String[] args) {
    StopwordUtil swUtil = new StopwordUtil(args[0]);
    if (swUtil.isStopWord(args[1])) {
      System.out.println("Stopword");
    }
  }
}
