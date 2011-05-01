import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class KmeansClustering {

	HashMap<String, HashMap<String, Double>> graph;
	HashMap<String, HashSet<String>> clusters;

	public KmeansClustering(HashMap<String, HashMap<String, Double>> graph,
			int numClusters, int numIter) {
		this.graph = graph;
		HashSet<String> centroids = getRandomCentroid(numClusters);
		clusters = new HashMap<String, HashSet<String>>();
		for (int i = 0; i < numIter; ++i) {
			Set<String> allWords = graph.keySet();
			for (String word : allWords) {
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
			clusters = null;
		}
	}

	private String getClosestWord(String word, HashSet<String> centroids) {
		String returnWord = null;
		Double maxSimilarity = 0.0;
		for (String centroid : centroids) {
			Double currentSimilarity = graph.get(centroid).get(word);
			if (currentSimilarity > maxSimilarity) {
				maxSimilarity = currentSimilarity;
				returnWord = centroid;
			}
		}
		return returnWord;
	}

	private String getCentroid(HashSet<String> clusterWords) {
		Double maxSimilarity = 0.0;
		String centroid = null;
		for (String clusterWord1 : clusterWords) {
			Double curCentroidSimilarity = 0.0;
			HashMap<String, Double> level2Map = graph.get(clusterWord1);
			if (level2Map == null) {
				continue;
			}
			for (String clusterWord2 : clusterWords) {
				if (clusterWord1.compareTo(clusterWord2) == 0) {
					continue;
				}
				curCentroidSimilarity += Math.log(level2Map.get(clusterWord2));
			}
			if (curCentroidSimilarity > maxSimilarity) {
				maxSimilarity = curCentroidSimilarity;
				centroid = clusterWord1;
			}
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
