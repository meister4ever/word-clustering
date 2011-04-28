import org.tartarus.snowball.ext.porterStemmer;

public class StringUtil {
	private static porterStemmer stemmer = new porterStemmer();

	static String stem(String string) {
		stemmer.setCurrent(string.trim().toLowerCase());
		stemmer.stem();
		return new String(stemmer.getCurrent());
	}

	static String clean(String string) {
		return string.trim().toLowerCase();
	}
}
