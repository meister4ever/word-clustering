import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Pointer;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.dictionary.Dictionary;

public class WordnetUtil {

	private static WordnetUtil wordnetUtil = null;
	private Dictionary dictionary;

	private WordnetUtil() throws FileNotFoundException, JWNLException {
		JWNL.initialize(new FileInputStream(
				"/home/rmenon/word-clustering/data/file_properties.xml"));
		dictionary = Dictionary.getInstance();
	}

	public static WordnetUtil getInstance() throws FileNotFoundException,
			JWNLException {
		if (wordnetUtil == null) {
			wordnetUtil = new WordnetUtil();
		}
		return wordnetUtil;
	}

	@SuppressWarnings("unchecked")
	public Set<String> getSynonyms(String word) throws JWNLException {
		Set<String> expansions = new TreeSet<String>();
		List<POS> allPos = POS.getAllPOS();
		for (POS pos : allPos) {
			IndexWord indexWord = dictionary.getIndexWord(pos, word);
			if (indexWord != null) {
				Synset[] synsets = indexWord.getSenses();
				for (Synset synset : synsets) {
					Word[] synWords = synset.getWords();
					for (Word synWord : synWords) {
						expansions.add(StringUtil.clean(synWord.getLemma()));
					}
					List<PointerType> allPtrs = PointerType.getAllPointerTypes();
					for (PointerType type : allPtrs) {
						expansions.addAll(getWordsFromDomainType(synset, type));
					}
				}
			}
		}
		return expansions;
	}

	// Return words from PointerTargetNodeList
	public Set<String> getWordsFromPtrNodeList(PointerTargetNodeList list)
			throws JWNLException {
		Set<String> words = new TreeSet<String>();
		if (list == null) {
			return words;
		}
		for (int i = 0; i < list.size(); ++i) {
			PointerTargetNode pt = (PointerTargetNode) list.get(i);
			if (pt != null) {
				Word word = pt.getWord();
				if (word != null) {
					words.add(StringUtil.clean(word.getLemma()));
				}
			}
		}
		return words;
	}

	// Given a set of synWords, get the domain category words from them.
	public Set<String> getWordsFromDomainType(Synset synset, PointerType type)
			throws JWNLException {
		Set<String> words = new TreeSet<String>();
		Pointer[] pointerArr = synset.getPointers(type);
		for (Pointer pointer : pointerArr) {
			Synset curSet = pointer.getTargetSynset();
			Word[] allWords = curSet.getWords();
			for (Word word : allWords) {
				words.add(StringUtil.clean(word.getLemma()));
			}
		}
		return words;
	}

	// Given a synset, return all associated words to this synset.
	public Set<String> getAssociatedWords(Synset synset) throws JWNLException {
		Set<String> words = new TreeSet<String>();
		PointerUtils ptUtils = PointerUtils.getInstance();

		// Also sees.
		words.addAll(getWordsFromPtrNodeList(ptUtils.getAlsoSees(synset)));
		// Get synonyms.
		words.addAll(getWordsFromPtrNodeList(ptUtils.getSynonyms(synset)));
		words.addAll(getWordsFromPtrNodeList(ptUtils.getAttributes(synset)));
		words.addAll(getWordsFromPtrNodeList(ptUtils.getAntonyms(synset)));
		words.addAll(getWordsFromPtrNodeList(ptUtils.getCauses(synset)));
		words.addAll(getWordsFromPtrNodeList(ptUtils.getAntonyms(synset)));
		return words;
	}

	public boolean isRelated(String word1, String word2) throws JWNLException {
		Set<String> words = getSynonyms(word1);
		return words.contains(word2);
	}

	public static void main(String[] args) throws JWNLException,
			FileNotFoundException {
		WordnetUtil wnUtil = WordnetUtil.getInstance();
		if (wnUtil.isRelated(args[0], args[1])) {
			System.out.println("Connected.");
		}
	}
}
