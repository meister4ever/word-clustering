import java.util.HashMap;
import java.util.HashSet;

public class CliqueFinder {

	private int connectionPerNode = 3;
	private HashMap<String, HashMap<String, Double>> adjList;

	public CliqueFinder(String filename, double scoreThreshold,
			int connectionPerNode) {
		this.connectionPerNode = connectionPerNode;

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
		while (removeEdges());
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
		return new HashSet<String>(adjList.get(word).keySet());
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
		if (P.size() == 0 && X.size() == 0) {
			System.out.println("=============");
			for (String word : R) {
				System.out.println(word);
			}
			System.out.println("=============");
		}
		for (String v : P) {
			HashSet<String> R1 = new HashSet<String>(R);
			R1.add(v);

			HashSet<String> nv = getNeighbours(v);
			HashSet<String> P1 = setIntersect(P, nv);
			HashSet<String> X1 = setIntersect(X, nv);

			dumpCliques1(R1, P1, X1);
			P.remove(v);
			X.add(v);
		}
	}

	public void dumpCliques() {
		HashSet<String> allNodes = new HashSet<String>(adjList.keySet());
		dumpCliques1(new HashSet<String>(), allNodes, new HashSet<String>());
	}
	
	public static void main(String[] args) {
		CliqueFinder cf = new CliqueFinder(args[0], 3.0, 5);
		cf.dumpCliques();
	}
}
