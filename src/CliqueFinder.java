import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.stat.clustering.KMeansPlusPlusClusterer;

public class CliqueFinder {

	private int connectionPerNode = 3;
  private String cooccurFile;
	private HashMap<String, HashMap<String, Double>> adjList;

	public CliqueFinder(String filename, double scoreThreshold,
			int connectionPerNode) {
		this.connectionPerNode = connectionPerNode;
    this.cooccurFile = filename;

		StreamingFileUtil fUtil = new StreamingFileUtil(filename);
		String line = null;
		adjList = new HashMap<String, HashMap<String, Double>>();
		while ((line = fUtil.getNextLine()) != null) {
			String[] parts = line.split("\t");
			String[] words = parts[0].split(":");
			String word1 = StringUtil.clean(words[0]);
			String word2 = StringUtil.clean(words[1]);
			Double score = Double.parseDouble(StringUtil.clean(parts[1]));
			if (score > scoreThreshold) {
				HashMap<String, Double> level2Map = adjList.get(word1);
				if (level2Map == null) {
					level2Map = new HashMap<String, Double>();
					adjList.put(word1, level2Map);
				}
				level2Map.put(word2, score);
			}
		}
		fUtil.close();

		// Create the cliques.
		while (removeEdges())
			;
	}

	private void removeNode(String word) {
		for (String word1 : adjList.keySet()) {
			if (word1.compareTo(word) == 0) {
				continue;
			}
			HashMap<String, Double> level2Map = adjList.get(word1);
			if (level2Map == null) {
				continue;
			}
			level2Map.remove(word);
		}
	}

	private boolean removeEdges() {
		int numRemoved = 0;
		for (String word1 : adjList.keySet()) {
			HashMap<String, Double> level2Map = adjList.get(word1);
			if (level2Map == null) {
				continue;
			}
			if (level2Map.size() < connectionPerNode) {
				adjList.put(word1, null);
				removeNode(word1);
				++numRemoved;
			}
		}
		return (numRemoved > 0);
	}

	private HashSet<String> getNeighbours(String word) {
		HashMap<String, Double> list = adjList.get(word);
		if (list == null) {
			return new HashSet<String>();
		}
		return new HashSet<String>(list.keySet());
	}

	private HashSet<String> setIntersect(HashSet<String> a, HashSet<String> b) {
		HashSet<String> intersect = new HashSet<String>();
		for (String word : a) {
			if (b.contains(word)) {
				intersect.add(word);
			}
		}

		return intersect;
	}

	// Implementation of Bron Kerbosch algorithm.
	private void dumpCliques1(HashSet<String> R, HashSet<String> P,
			HashSet<String> X) {
		if (R.size() >= 20 && (P.size() == 0 && X.size() == 0)) {
			System.out.println("=============");
			for (String word : R) {
				System.out.println(word);
			}
			System.out.println("=============");
		}
		HashSet<String> tmpP = new HashSet<String>(P);
		for (String v : tmpP) {
			HashSet<String> R1 = new HashSet<String>(R);
			R1.add(v);

			HashSet<String> nv = getNeighbours(v);
			nv.remove(v);
			HashSet<String> P1 = setIntersect(P, nv);
			HashSet<String> X1 = setIntersect(X, nv);

			dumpCliques1(R1, P1, X1);
			P.remove(v);
			X.add(v);
		}
	}

	private boolean isClique(String word) {
		HashSet<String> nodes = getNeighbours(word);
		nodes.add(word);
		for (String node1 : nodes) {
			for (String node2 : nodes) {
				if (node1.compareTo(node2) == 0) {
					continue;
				}
				HashMap<String, Double> level2Map = adjList.get(node1);
				if (level2Map == null || !level2Map.containsKey(node2)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean removeAndDumpClique() {
		boolean removed = false;
		for (String word : adjList.keySet()) {
			if (isClique(word)) {
				System.out.print(word);
				HashMap<String, Double> level2Map = adjList.get(word);
				if (level2Map != null)
					for (String adj : level2Map.keySet()) {
						System.out.print(adj);
					}
				System.out.println("\n");
				removed = true;
			}
		}
		return removed;
	}

	public void doBfs(String word, HashSet<String> visited) {
		Queue<String> queue = new LinkedList<String>();
		queue.add(word);
		while (queue.size() > 0) {
			String aNode = queue.remove();
			visited.add(aNode);
			System.out.print(aNode + ", ");
			HashSet<String> neighbours = getNeighbours(aNode);
			for (String neighbour : neighbours) {
				if (!visited.contains(neighbour)) {
					queue.add(neighbour);
				}
			}
		}
	}

	public void dumpCliques() {
		// HashSet<String> allNodes = new HashSet<String>(adjList.keySet());
		// dumpCliques1(new HashSet<String>(), allNodes, new HashSet<String>());
		// while (removeAndDumpClique());

		/*
		 * HashSet<String> visited = new HashSet<String>(); for (String word :
		 * adjList.keySet()) { if (!visited.contains(word)) { doBfs(word,
		 * visited); System.out.println("\n"); } }
		 */
		KmeansClustering kmeans = new KmeansClustering(adjList, cooccurFile, 50, 10);
		kmeans.dumpClusters();
	}

	public void dumpGraph() {
		HashMap<String, Double> allPairs = new HashMap<String, Double>();
		for (String word1 : adjList.keySet()) {
			HashMap<String, Double> level2Map = adjList.get(word1);
			if (level2Map == null) {
				continue;
			}
			for (String word2 : level2Map.keySet()) {
				String key = "\"" + word1 + "\"" + " -- " + "\"" + word2 + "\"";
				if (!allPairs.containsKey(key)) {
					allPairs.put(key, level2Map.get(word2));
				}
			}
		}
		System.out.println("graph G {");
		for (String key : allPairs.keySet()) {
			System.out.println("\t" + key /*
										 * + "[ label=\"" + allPairs.get(key) +
										 * "\"];"
										 */);
		}
		System.out.println("}");
	}

	public void printGraphStats() {
		for (String word1 : adjList.keySet()) {
			HashMap<String, Double> level2Map = adjList.get(word1);
			if (level2Map == null) {
				continue;
			}
			System.out.println(word1 + "\t" + level2Map.size());
		}
	}

	public static void main(String[] args) {
		CliqueFinder cf = new CliqueFinder(args[0],
				Double.parseDouble(args[1]), Integer.parseInt(args[2]));
		cf.dumpCliques();
		// cf.printGraphStats();
		// cf.dumpGraph();
	}
}
