import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class TopicWordMap {
	private HashMap<String, HashSet<Integer>> topicWordsMap = new HashMap<String, HashSet<Integer>>();
	private HashMap<Integer, HashSet<String>> wordTopicsMap = new HashMap<Integer, HashSet<String>>();
	
	TopicWordMap(String filename, WordIdMap wordIdMap) {
		String fileContent = FileUtil.ReadFileContent(filename);
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split(":");
      if (parts.length < 2) {
        continue;
      }
			String topic = StringUtil.clean(parts[0]);
			HashSet<Integer> topicWordIndices = new HashSet<Integer>();
			Integer topicIdx = wordIdMap.getIndex(topic);
			if (topicIdx != null) {
				topicWordIndices.add(topicIdx);
				HashSet<String> topics = wordTopicsMap.get(topicIdx);
				if (topics == null) {
					topics = new HashSet<String>();
					wordTopicsMap.put(topicIdx, topics);
				}
				topics.add(topic);
			}
			String[] topicWordStrings = parts[1].split(",");
			for (String topicWordString : topicWordStrings) {
				Integer topicWordIdx = wordIdMap.getIndex(topicWordString);
				if (topicWordIdx != null) {
					topicWordIndices.add(topicWordIdx);
					HashSet<String> topics = wordTopicsMap.get(topicWordIdx);
					if (topics == null) {
						topics = new HashSet<String>();
						wordTopicsMap.put(topicWordIdx, topics);
					}
					topics.add(topic);
				}
			}
			topicWordsMap.put(topic, topicWordIndices);
		}
	}
	
	HashSet<String> getTopicsForWordId(Integer wordId) {
		return wordTopicsMap.get(wordId);
	}
	
	Set<String> getAllTopics() {
		return topicWordsMap.keySet(); 
	}

  HashSet<Integer> getWordIdsForTopics(String topic) {
    return topicWordsMap.get(topic);
  }
}
