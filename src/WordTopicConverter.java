import java.lang.Math;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class WordTopicConverter {

	private TopicWordMap topicWordMap;
	private Set<Integer> wordIndices;
	private Set<String> allTopics;
  private WordIdMap wordIdMap;
	
	public WordTopicConverter(WordIdMap wordIdMap, TopicWordMap topicWordMap) {
		super();
		this.topicWordMap = topicWordMap;
		wordIndices = wordIdMap.getAllWordIndices();
		allTopics = topicWordMap.getAllTopics();
    this.wordIdMap = wordIdMap;
	}
	
	String convertWords2Topics(String line) {
		Integer pos = line.indexOf(':');
		String entityName = line.substring(0, pos);
		String[] parts = line.substring(pos + 1).split(",");
		
		HashMap<Integer, Integer> freqMap = new HashMap<Integer, Integer>();
		Long totalFreq = new Long(wordIndices.size());
    Double cutoffProb = 1.0 / totalFreq;
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
		HashMap<String, Double> topicExpMap = new HashMap<String, Double>();
		HashMap<String, Integer> topicSpreadMap = new HashMap<String, Integer>();
		for (String topic : allTopics) {
			//topicProbMap.put(topic, 1.0/(double)totalFreq);
			topicProbMap.put(topic, 0.0);
			topicExpMap.put(topic, 0.0);
		}
		
		for (Integer wordIdx : wordIndices) {
			HashSet<String> topics = topicWordMap.getTopicsForWordId(wordIdx);
      if (topics == null) {
        continue;
      }
      Integer curWordCnt = freqMap.get(wordIdx);
			Double wordProb = (double)curWordCnt/(double)totalFreq;
			for (String topic : topics) {

        Double expFactor = 0.0;
        if (wordProb > 0.0) {
          expFactor = wordProb / wordIdMap.getProb(wordIdx);
        }

				Double curProb = topicProbMap.get(topic);
				topicProbMap.put(topic, curProb + wordProb);
        Double curExp = topicExpMap.get(topic);
        topicExpMap.put(topic, curExp + wordIdMap.getProb(wordIdx));
        if (curWordCnt > 10 && expFactor > 1.0) {
          /*
          if (wordProb > 0.0) {
            System.out.println(wordIdMap.getWord(wordIdx) + "\t" + topic + "\t" + freqMap.get(wordIdx) + "\t" + wordProb + "\t" + wordIdMap.getProb(wordIdx));
          } else {
            System.out.println(wordIdMap.getWord(wordIdx) + "\t" + topic + "\t" + freqMap.get(wordIdx) + "\t" + wordProb);
          }
          */

          Integer curCnt = topicSpreadMap.get(topic);
          if (curCnt == null) {
            curCnt = 0;
          }
          topicSpreadMap.put(topic, curCnt + 1);
        }
			}
		}
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String topic : allTopics) {
			if (!first) {
				sb.append(",");
			}
			first = false;
      if (topicProbMap.get(topic) == 0.0) {
        sb.append(topic + ":" + topicProbMap.get(topic));
      } else {
        Integer spread = topicSpreadMap.get(topic);
        if (spread == null) {
          spread = 1;
        }
        Double spreadWeight = 1.0 / Math.exp(25.0 / Math.pow(spread, 2));
        sb.append(topic + ":" + topicProbMap.get(topic) * spreadWeight / topicExpMap.get(topic) );
      }
		}
		return sb.toString();
	}
}
