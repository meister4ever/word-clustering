import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class KmeansClustering {

	HashMap<String, HashMap<String, Double>> graph;
	HashMap<String, HashSet<String>> clusters;
	private HashMap<String, HashMap<String, Double>> adjList;

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
		clusters = new HashMap<String, HashSet<String>>();
		for (int i = 0; i < numIter; ++i) {
      for (String centroid : centroids) {
        System.out.print(centroid + ", ");
      }
      System.out.println("\n");
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
				String closestWord = getClosestWord(word, centroids);
				HashSet<String> clusterWords = clusters.get(closestWord);
				if (clusterWords == null) {
					clusterWords = new HashSet<String>();
					clusters.put(closestWord, clusterWords);
				}
				clusterWords.add(word);
			}
			if (i + 1 == numIter) {
				break;
			}
			centroids = getNewCentroids(clusters);
			clusters = new HashMap<String, HashSet<String>>();
		}
	}

	private String getClosestWord(String word, HashSet<String> centroids) {
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
		return returnWord;
	}

	private String getCentroid(HashSet<String> clusterWords) {
		Double maxSimilarity = 0.0;
		String centroid = null;
		for (String clusterWord1 : clusterWords) {
			Double curCentroidSimilarity = 0.0;
			HashMap<String, Double> level2Map = adjList.get(clusterWord1);
			for (String clusterWord2 : clusterWords) {
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
			HashMap<String, HashSet<String>> clusters) {
		HashSet<String> newCentroids = new HashSet<String>();
		for (String centroid : clusters.keySet()) {
			HashSet<String> clusterWords = clusters.get(centroid);
			if (clusterWords == null) {
				clusterWords = new HashSet<String>();
				clusters.put(centroid, clusterWords);
			}
			clusterWords.add(centroid);
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
			HashSet<String> clusterWords = clusters.get(centroid);
			if (clusterWords == null) {
				continue;
			}
			for (String clusterWord : clusterWords) {
				System.out.print(clusterWord + ", ");
			}
			System.out.println("\n");
		}
	}
}
