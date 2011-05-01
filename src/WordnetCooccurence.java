import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import net.didion.jwnl.JWNLException;

import org.apache.commons.math.linear.OpenMapRealMatrix;

public class WordnetCooccurence {
	
	private ArrayList<String> words;
	private OpenMapRealMatrix newMat;
	
	public WordnetCooccurence(String filename, int pathLen) throws FileNotFoundException, JWNLException {
		String textStr = FileUtil.ReadFileContent(filename);
		String[] lines = textStr.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\t");
			words.add(StringUtil.clean(parts[0]));
		}
		
		WordnetUtil wnUtil = WordnetUtil.getInstance();
		
		HashMap<String, Integer> numChildMap = new HashMap<String, Integer>();
		// Create graph
		int numWords = words.size();
		OpenMapRealMatrix adjMat = new OpenMapRealMatrix(numWords, numWords);
		for (int i = 0; i < numWords; ++i) {
			int numChildren = 0;
			for (int j = 0; j < numWords; ++j) {
				if ( i == j) {
					adjMat.setEntry(i, j, 0.0);
				}
				if (wnUtil.isRelated(words.get(i), words.get(j))) {
					adjMat.setEntry(i, j, 1.0);
					++numChildren;
				}
			}
			numChildMap.put(words.get(i), numChildren);
		}
		
		newMat = new OpenMapRealMatrix(adjMat);
		for (int i = 0; i < pathLen-1; ++i) {
			newMat = newMat.multiply(adjMat);
		}
		
		for (int i = 0; i < numWords; ++i) {
			double word1Freq = (double)numChildMap.get(words.get(i));
			for (int j = 0; j < numWords; ++j) {
				if (i == j) {
					continue;
				}
				double word2Freq = (double)numChildMap.get(words.get(j));
				double factor = 1.0/(word1Freq * word2Freq);
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
					System.out.println(words.get(i) + ":" + words.get(j) + "\t" + value);
				}
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, JWNLException {
		WordnetCooccurence wnCooccur = new WordnetCooccurence(args[0], 3);
		wnCooccur.dumpGraph();
	}
}
