import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import net.didion.jwnl.JWNLException;

import org.apache.commons.math.linear.OpenMapRealMatrix;

public class WordnetGraph {

	private ArrayList<String> words = new ArrayList<String>();
	private HashMap<String, Integer> wordIndex = new HashMap<String, Integer>();
	private OpenMapRealMatrix newMat;

	public WordnetGraph(String wordFreqFile, String cooccurFile, int pathLen)
			throws FileNotFoundException, JWNLException {
		String textStr = FileUtil.ReadFileContent(wordFreqFile);
		String[] lines = textStr.split("\n");

		int i = 0;
		for (String line : lines) {
			String[] parts = line.split("\t");
			String cleanedWord = StringUtil.clean(parts[0]);
			words.add(cleanedWord);
			wordIndex.put(cleanedWord, i++);
		}

		int numWords = words.size();
		OpenMapRealMatrix adjMat = new OpenMapRealMatrix(numWords, numWords);
		textStr = FileUtil.ReadFileContent(cooccurFile);
		lines = textStr.split("\n");
		HashMap<Integer, Integer> numChildrenMap = new HashMap<Integer, Integer>();
		int count = 0;
		for (String line : lines) {
			String[] parts = line.split("\t");
			String[] words = parts[0].split(":");
			String word1 = StringUtil.clean(words[0]);
			String word2 = StringUtil.clean(words[1]);
			Integer word1Idx = wordIndex.get(word1);
			Integer word2Idx = wordIndex.get(word2);
			adjMat.setEntry(word1Idx, word2Idx, 1.0);
			Integer children = numChildrenMap.get(word1Idx);
			if (children == null) {
				numChildrenMap.put(word1Idx, 1);
			} else {
				numChildrenMap.put(word1Idx, children + 1);
			}
			++count;
			System.err.print("Done: " + count + "/" + numWords + "\r");
		}

		newMat = new OpenMapRealMatrix(adjMat);
		for (i = 0; i < pathLen - 1; ++i) {
			newMat = newMat.multiply(adjMat);
		}

		for (i = 0; i < numWords; ++i) {
			double word1Freq = (double) numChildrenMap.get(i);
			for (int j = 0; j < numWords; ++j) {
				if (i == j) {
					continue;
				}
				double word2Freq = (double) numChildrenMap.get(j);
				double factor = 1.0 / (word1Freq * word2Freq);
				newMat.multiplyEntry(i, j, factor);
			}
		}
	}

	public void dumpGraph() {
		int numWords = words.size();
		for (int i = 0; i < numWords; ++i) {
			for (int j = 0; j < numWords; ++j) {
				Double value = newMat.getEntry(i, j);
				if (value > 0.0) {
					System.out.println(words.get(i) + ":" + words.get(j) + "\t"
							+ value);
				}
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException,
			JWNLException {
		WordnetGraph wnCooccur = new WordnetGraph(args[0], args[1], 3);
		wnCooccur.dumpGraph();
	}
}
