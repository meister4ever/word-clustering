import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class KmeansClustering {

	HashMap<String, HashMap<String, Double>> graph;
	HashMap<String, TreeSet<WordScore>> clusters;
	private HashMap<String, HashMap<String, Double>> adjList;

	public static class WordScore implements Comparable {
		private String word;
		private Double score;

		WordScore(String word, Double score) {
			this.word = word;
			this.score = score;
		}

		@Override
		public int compareTo(Object other) {
			WordScore otherWs = (WordScore) other;
			if (score < otherWs.score) {
				return 1;
			} else if (score > otherWs.score) {
				return -1;
			} else {
				return this.word.compareTo(otherWs.word);
			}
		}
	}

	public KmeansClustering(HashMap<String, HashMap<String, Double>> graph,
			String filename, int numClusters, int numIter) {
		this.graph = graph;

		StreamingFileUtil fUtil = new StreamingFileUtil(filename);
		String line = null;
		adjList = new HashMap<String, HashMap<String, Double>>();
		while ((line = fUtil.getNextLine()) != null) {
			String[] parts = line.split("\t");
			String[] words = parts[0].split(":");
			String word1 = StringUtil.clean(words[0]);
			String word2 = StringUtil.clean(words[1]);
			Double score = Double.parseDouble(StringUtil.clean(parts[1]));
			HashMap<String, Double> level2Map = adjList.get(word1);
			if (level2Map == null) {
				level2Map = new HashMap<String, Double>();
				adjList.put(word1, level2Map);
			}
			level2Map.put(word2, score);
		}
		fUtil.close();

		HashSet<String> centroids = getRandomCentroid(numClusters);
		clusters = new HashMap<String, TreeSet<WordScore>>();
		for (int i = 0; i < numIter; ++i) {
			/*for (String centroid : centroids) {
				System.out.print(centroid + ", ");
			}
			System.out.println("\n");
      */
      System.err.println("Iter = " + i+1 + "\r");
			Set<String> allWords = graph.keySet();
			for (String word : allWords) {
				if (graph.get(word) == null) {
					continue;
				}
				if (graph.get(word).size() == 0) {
					continue;
				}
				if (centroids.contains(word)) {
					continue;
				}
				WordScore closestWord = getClosestWord(word, centroids);
				TreeSet<WordScore> clusterWords = clusters.get(closestWord.word);
				if (clusterWords == null) {
					clusterWords = new TreeSet<WordScore>();
					clusters.put(closestWord.word, clusterWords);
				}
				clusterWords.add(new WordScore(word, closestWord.score));
			}
			if (i + 1 == numIter) {
				break;
			}
			centroids = getNewCentroids(clusters);
			clusters = new HashMap<String, TreeSet<WordScore>>();
		}
	}

	private WordScore getClosestWord(String word, HashSet<String> centroids) {
		String returnWord = null;
		Double maxSimilarity = 0.0;
		for (String centroid : centroids) {
			HashMap<String, Double> level2Map = adjList.get(centroid);
			Double currentSimilarity = 0.0;
			if (level2Map != null) {
				currentSimilarity = level2Map.get(word);
				if (currentSimilarity == null) {
					currentSimilarity = 0.0;
				}
			}
			if (currentSimilarity >= maxSimilarity) {
				maxSimilarity = currentSimilarity;
				returnWord = centroid;
			}
		}
		if (returnWord == null) {
			System.err.println("ERR: (getClosestWord): " + word);
		}
		return new WordScore(returnWord, maxSimilarity);
	}

	private String getCentroid(TreeSet<WordScore> clusterWords) {
		Double maxSimilarity = 0.0;
		String centroid = null;
		for (WordScore clusterWordScore1 : clusterWords) {
			String clusterWord1 = clusterWordScore1.word;
			Double curCentroidSimilarity = 0.0;
			HashMap<String, Double> level2Map = adjList.get(clusterWord1);
			for (WordScore clusterWordScore2 : clusterWords) {
				String clusterWord2 = clusterWordScore2.word;
				if (clusterWord1.compareTo(clusterWord2) == 0) {
					continue;
				}
				Double curSim = 0.0;
				if (level2Map != null) {
					curSim = level2Map.get(clusterWord2);
					if (curSim == null) {
						curSim = 0.0;
					}
				}
				curCentroidSimilarity += curSim;
			}
			if (curCentroidSimilarity >= maxSimilarity) {
				maxSimilarity = curCentroidSimilarity;
				centroid = clusterWord1;
			}
		}

		if (centroid == null) {
			System.err.println("ERR: (getCentroid): ");
		}
		return centroid;
	}

	private HashSet<String> getNewCentroids(
			HashMap<String, TreeSet<WordScore>> clusters) {
		HashSet<String> newCentroids = new HashSet<String>();
		for (String centroid : clusters.keySet()) {
			TreeSet<WordScore> clusterWords = clusters.get(centroid);
			if (clusterWords == null) {
				clusterWords = new TreeSet<WordScore>();
				clusters.put(centroid, clusterWords);
			}
			clusterWords.add(new WordScore(centroid, 0.0));
			newCentroids.add(getCentroid(clusterWords));
		}
		return newCentroids;
	}

	HashSet<String> getRandomCentroid(int numClusters) {
		HashSet<String> centroids = new HashSet<String>();
		int i = 0;
		for (String word : graph.keySet()) {
			centroids.add(word);
			if (++i > numClusters) {
				break;
			}
		}
		return centroids;
	}

	public void dumpClusters() {
		for (String centroid : clusters.keySet()) {
			System.out.print(centroid + ":");
			TreeSet<WordScore> clusterWords = clusters.get(centroid);
			if (clusterWords == null) {
				continue;
			}
			for (WordScore clusterWord : clusterWords) {
				System.out.print(clusterWord.word + ", ");
			}
			System.out.println("\n");
		}
	}
}
