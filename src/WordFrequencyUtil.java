import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;


public class WordFrequencyUtil {
  private HashMap<String, Integer> wordFreq = new HashMap<String, Integer>();
  
  public WordFrequencyUtil(String filename, Configuration conf) {
    String textStr = HdfsFileUtil.ReadFileContent(filename, conf);
    String[] lines = textStr.split("\n");
    for (String line : lines) {
      String[] parts = line.split("\t");
      String stemmedWord = StringUtil.stem(parts[0]);
      Integer freq = Integer.parseInt(StringUtil.clean(parts[1]));
      wordFreq.put(stemmedWord, freq);
    }
  }
  
  public Integer getFrequency(String word) {
    String stemmedWord = StringUtil.stem(word);
    Integer value = wordFreq.get(stemmedWord);
    if (value == null) {
      value = 0;
    }
    return value;
  }
}
