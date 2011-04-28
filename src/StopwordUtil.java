import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.tartarus.snowball.ext.porterStemmer;

public class StopwordUtil {
	private HashSet<String> stopwordSet;
	private porterStemmer stemmer;
	
	public StopwordUtil(String fileName, Configuration conf) {
		stopwordSet = new HashSet<String>();
		stemmer = new porterStemmer();
		String stopwordStrings = HdfsFileUtil.ReadFileContent(fileName, conf);
		for (String stopword : stopwordStrings.split("\n")) {
			stemmer.setCurrent(stopword.toLowerCase());
			stemmer.stem();
			stopwordSet.add(stemmer.getCurrent());
		}
	}
	
	public boolean isStopWord(String word) {
		stemmer.setCurrent(word.toLowerCase());
		stemmer.stem();
		String stemmed = stemmer.getCurrent();
		return stopwordSet.contains(stemmed);
	}
}
