import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class WordTopicConverter {

	private TopicWordMap topicWordMap;
	private Set<Integer> wordIndices;
	private Set<String> allTopics;
	
	public WordTopicConverter(WordIdMap wordIdMap, TopicWordMap topicWordMap) {
		super();
		this.topicWordMap = topicWordMap;
		wordIndices = wordIdMap.getAllWordIndices();
		allTopics = topicWordMap.getAllTopics();
	}
	
	String convertWords2Topics(String line) {
		Integer pos = line.indexOf(':');
		String entityName = line.substring(0, pos);
		String[] parts = line.substring(pos + 1).split(",");
		
		HashMap<Integer, Integer> freqMap = new HashMap<Integer, Integer>();
		Long totalFreq = new Long(wordIndices.size());
		for (Integer wordIdx : wordIndices) {
			freqMap.put(wordIdx, 1);
		}
		
		for (String wordFreq : parts) {
			String[] wordParts = wordFreq.split(":");
			Integer wordIdx = Integer.parseInt(StringUtil.clean(wordParts[0]));
			Integer frequency = Integer.parseInt(StringUtil.clean(wordParts[1]));
			if (wordIndices.contains(wordIdx)) {
				Integer curFreq = freqMap.get(wordIdx);
				freqMap.put(wordIdx, curFreq + frequency);
				totalFreq += frequency;
			}
		}
		
		HashMap<String, Double> topicProbMap = new HashMap<String, Double>();
		for (String topic : allTopics) {
			topicProbMap.put(topic, 1.0/(double)totalFreq);
		}
		
		for (Integer wordIdx : wordIndices) {
			HashSet<String> topics = topicWordMap.getTopicsForWordId(wordIdx);
      if (topics == null) {
        continue;
      }
			Double wordProb = (double)freqMap.get(wordIdx)/(double)totalFreq;
			for (String topic : topics) {
				Double curProb = topicProbMap.get(topic);
				topicProbMap.put(topic, curProb + wordProb);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String topic : allTopics) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append(topic + ":" + topicProbMap.get(topic));
		}
		return sb.toString();
	}
}
