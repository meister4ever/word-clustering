import java.util.HashMap;
import java.util.Set;

public class WordIdMap {
	private HashMap<Integer, String> indexWordMap = new HashMap<Integer, String>();
	private HashMap<String, Integer> wordIndexMap = new HashMap<String, Integer>();
	
	WordIdMap(String filename, StopwordUtil swUtil) {
		String fileContent = FileUtil.ReadFileContent(filename);
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\t");
			String word = StringUtil.clean(parts[0]);
			String stemmedWord = StringUtil.stem(word);
			Integer idx = Integer.parseInt(StringUtil.clean(parts[1]));
			if (swUtil.isStopWord(word)) {
				continue;
			}
			indexWordMap.put(idx, word);
			wordIndexMap.put(stemmedWord, idx);
		}
	}
	
	Set<Integer> getAllWordIndices() {
		return indexWordMap.keySet();
	}
	
	String getWord(Integer idx) {
		return indexWordMap.get(idx);
	}
	
	Integer getIndex(String word) {
		String stemmedWord = StringUtil.stem(word);
		return wordIndexMap.get(stemmedWord);
	}
}
