import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class TopicExpander {

	private static class WordScore implements Comparable<WordScore> {
		public String word;
		public Double score;

		public WordScore(String word, Double score) {
			super();
			this.word = word;
			this.score = score;
		}

		@Override
		public int compareTo(WordScore otherWordScore) {
			if (this.score < otherWordScore.score) {
				return -1;
			} else if (this.score > otherWordScore.score) {
				return 1;
			} else {
				return this.word.compareTo(otherWordScore.word);
			}
		}

	}

	private HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
	private WordFrequencyUtil wfUtil;
	private HashMap<String, HashMap<String, Double>> adjList;
	private HashMap<String, TreeSet<WordScore>> clusters = new HashMap<String, TreeSet<WordScore>>();
	private HashMap<String, TreeSet<WordScore>> weakClusters = new HashMap<String, TreeSet<WordScore>>();
	private static final Long NUM_WORDS = (long) 1594793586;
	private double maxFreqThreshold = 1.0 / 100.0;
	private double minFreqThreshold = 1.0 / 5000.0;
	private double affinityThreshold = 0.5;

	TopicExpander(String seedWordsFile, String graphFile, String freqFile,
			double edgeThreshold, double minFreqThreshold,
			double maxFreqThreshold, double affinityThreshold) {

		this.maxFreqThreshold = maxFreqThreshold;
		this.minFreqThreshold = minFreqThreshold;
		this.affinityThreshold = affinityThreshold;

		// Read the frequency into the word frequency file.
		wfUtil = new WordFrequencyUtil(freqFile);
		StreamingFileUtil fUtil = new StreamingFileUtil(graphFile);

		// Fill up the word graph.
		String line = null;
		adjList = new HashMap<String, HashMap<String, Double>>();
		while ((line = fUtil.getNextLine()) != null) {
			String[] parts = line.split("\t");
			String[] words = parts[0].split(":");
			String word1 = StringUtil.clean(words[0]);
			String word2 = StringUtil.clean(words[1]);
			Double score = Double.parseDouble(StringUtil.clean(parts[1]));
			if (score > edgeThreshold) {
				HashMap<String, Double> level2Map = adjList.get(word1);
				if (level2Map == null) {
					level2Map = new HashMap<String, Double>();
					adjList.put(word1, level2Map);
				}
				level2Map.put(word2, score);
			}
		}
		fUtil.close();

		// The streaming file for the seed words.
		fUtil = new StreamingFileUtil(seedWordsFile);
		long maxFreq = (long) (NUM_WORDS * this.maxFreqThreshold);
		long minFreq = (long) (NUM_WORDS * this.minFreqThreshold);

		// Fill up the seed words.
		while ((line = fUtil.getNextLine()) != null) {
			String[] parts = line.split(":");
			String topicWord = StringUtil.clean(parts[0]);
			TreeSet<WordScore> clusterWords = new TreeSet<WordScore>();
			clusters.put(topicWord, clusterWords);
			clusterWords.add(new WordScore(topicWord, 1.0));
			String[] otherWords = parts[1].split(",");
			for (String word : otherWords) {
				clusterWords.add(new WordScore(StringUtil.clean(word), 1.0));
			}
		}

		// For each word, try to assign it to best k clusters.
		fUtil = new StreamingFileUtil(freqFile);
		line = null;
		while ((line = fUtil.getNextLine()) != null) {
			String currentWord = StringUtil.clean(line.split("\t")[0]);
			TreeSet<WordScore> topics = getBestKClustersForWord(currentWord,
					10, 3, maxFreq);
			for (WordScore ws : topics) {
				String topic = ws.word;
				clusters.get(topic).add(new WordScore(currentWord, ws.score));
			}
		}

		// Filter the clusters which do not have at least minFreq frequency.
		Set<String> topics = clusters.keySet();
		for (String topic : topics) {
			long clusterFrequency = (long) (getClusterFrequency(clusters
					.get(topic)));
			if (clusterFrequency < minFreq) {
				weakClusters.put(topic, clusters.get(topic));
				clusters.remove(topic);
			}
		}
	}

	long getClusterFrequency(TreeSet<WordScore> clusterWords) {
		long freq = 0;
		for (WordScore ws : clusterWords) {
			freq += (long) (wfUtil.getFrequency(ws.word));
		}
		return freq;
	}

	TreeSet<WordScore> getBestKClustersForWord(String currentWord,
			Integer maxAllotment, int minConnections, long maxFreq) {
		TreeSet<WordScore> bestClusters = new TreeSet<TopicExpander.WordScore>();

		for (String topic : clusters.keySet()) {
			TreeSet<WordScore> clusterWords = clusters.get(topic);
			long curFreq = getClusterFrequency(clusterWords);
			long wordFreq = wfUtil.getFrequency(currentWord);
			// Check if adding the new word increases the cluster frequency more
			// than max.
			if (curFreq + wordFreq <= maxFreq) {
				Integer curCount = wordCount.get(currentWord);
				if (curCount == null) {
					wordCount.put(currentWord, 1);
				} else {
					wordCount.put(currentWord, curCount + 1);
				}
				double clusterAffinity = getClusterAffinity(currentWord,
						clusterWords, minConnections);
				if (clusterAffinity >= affinityThreshold) {
					bestClusters.add(new WordScore(topic, clusterAffinity));
					// If the word is part of more than maxAllotment number of
					// clusters,
					// remove the least close cluster.
					if (bestClusters.size() > maxAllotment) {
						bestClusters.remove(bestClusters.last());
					}
				}
			}
		}
		return bestClusters;
	}

	double getClusterAffinity(String currentWord,
			TreeSet<WordScore> clusterWords, int minConnections) {
		double affinity = 0.0;
		int connections = 0;
		HashMap<String, Double> level2Map = adjList.get(currentWord);
		if (level2Map != null) {
			for (WordScore clusterWord : clusterWords) {
				Double score = level2Map.get(clusterWord);
				if (score != null) {
					affinity += score;
					connections++;
				}
			}
		}
		if (connections < minConnections) {
			affinity = 0.0;
		}
		return affinity;
	}

	public void printClusters() {
		for (String topic : clusters.keySet()) {
			System.out.println(topic);
			for (WordScore wordScore : clusters.get(topic)) {
				System.out.print(wordScore.word + ",");
			}
			System.out.println("\n");
		}
	}

	public void printWeakClusters() {
		for (String topic : weakClusters.keySet()) {
			System.out.println(topic);
			for (WordScore wordScore : weakClusters.get(topic)) {
				System.out.print(wordScore.word + ",");
			}
			System.out.println("\n");
		}
	}

	public static void main(String[] args) {
		String seedWordsFile = args[0];
		String graphFile = args[1];
		String freqFile = args[2];
		double edgeThreshold = Double.parseDouble(args[3]);
		double minFreqThreshold = Double.parseDouble(args[4]);
		double maxFreqThreshold = Double.parseDouble(args[5]);
		double affinityThreshold = Double.parseDouble(args[6]);
		TopicExpander expander = new TopicExpander(seedWordsFile, graphFile,
				freqFile, edgeThreshold, minFreqThreshold, maxFreqThreshold,
				affinityThreshold);
		System.out.println("===================");
		System.out.println("Strong Clusters");
		System.out.println("===================");
		expander.printClusters();
		System.out.println("===================");
		System.out.println("Weak Clusters");
		System.out.println("===================");
		expander.printWeakClusters();
	}
}
