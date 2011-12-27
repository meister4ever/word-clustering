import java.util.HashMap;
import java.util.Set;

public class WordIdMap {
	private HashMap<Integer, String> indexWordMap = new HashMap<Integer, String>();
	private HashMap<String, Integer> wordIndexMap = new HashMap<String, Integer>();
	private HashMap<Integer, Double> wordIndexProbMap = new HashMap<Integer, Double>();
	
	WordIdMap(String filename, String probFilename, StopwordUtil swUtil) {
		String fileContent = FileUtil.ReadFileContent(filename);
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\t");
			String word = StringUtil.clean(parts[0]);
			String stemmedWord = word;//StringUtil.stem(word);
			Integer idx = Integer.parseInt(StringUtil.clean(parts[1]));
			if (swUtil.isStopWord(word)) {
				continue;
			}
			indexWordMap.put(idx, word);
			wordIndexMap.put(stemmedWord, idx);
		}

    fileContent = FileUtil.ReadFileContent(probFilename);
		lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\t");
			Integer wordIdx = Integer.parseInt(StringUtil.clean(parts[0]));
			Double prob = Double.parseDouble(StringUtil.clean(parts[1]));
			wordIndexProbMap.put(wordIdx, prob);
		}
	}
	
	Set<Integer> getAllWordIndices() {
		return indexWordMap.keySet();
	}
	
	String getWord(Integer idx) {
		return indexWordMap.get(idx);
	}
	
	Integer getIndex(String word) {
		//String stemmedWord = StringUtil.stem(word);
		String stemmedWord = StringUtil.clean(word);
		return wordIndexMap.get(stemmedWord);
	}

  Double getProb(Integer wordIdx) {
    return wordIndexProbMap.get(wordIdx);
  }

  Double getProb(String word) {
    return getProb(getIndex(word));
  }
}
