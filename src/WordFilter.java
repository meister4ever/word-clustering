import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.tartarus.snowball.ext.porterStemmer;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class WordFilter {
	private StopwordUtil swUtil;
	private porterStemmer stemmer = new porterStemmer();
	private MaxentTagger tagger;
	
	public WordFilter(String stopwordFile, String taggerFile,
			Configuration conf) throws IOException, ClassNotFoundException {
		swUtil = new StopwordUtil(stopwordFile, conf);
		tagger = new MaxentTagger(taggerFile);
	}
	
	public boolean dropWord(String word) {
		stemmer.setCurrent(word.toLowerCase());
		stemmer.stem();
		String stemmedWord = stemmer.getCurrent();
		
		// Check if the word is long or zero length.
		if (word.length() > 20 || word.length() == 0) {
			return true;
		}
		
		// Check if the word has non alphabets.
		if (stemmedWord.matches(".*[^a-z].*")) {
			return true;
		}
		
		// Check if the word is a stopword.
		if (swUtil.isStopWord(word)) {
			return true;
		}
		
		// Check if the word is a noun or adjective.
		String pos = null;
		List<ArrayList<? extends HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(word));
		for (ArrayList<? extends HasWord> sentence : sentences) {
            ArrayList<TaggedWord> tSentence = this.tagger.tagSentence(sentence);
            for (TaggedWord tWord : tSentence) {
            	pos = tWord.tag();
            }
		}
		if (!pos.startsWith("NN") &&
			!pos.startsWith("NP") &&
			!pos.startsWith("NR") &&
			!pos.startsWith("RB") &&
			!pos.startsWith("V") &&
			!pos.startsWith("JJ")) {
			return true;
		}
			
		return false;
	}
}