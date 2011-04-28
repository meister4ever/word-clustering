import org.tartarus.snowball.ext.porterStemmer;

public class StringUtil {
	private static porterStemmer stemmer = new porterStemmer();
	static String Stem(String string) {
		stemmer.setCurrent(string.toLowerCase());
		stemmer.stem();
		return stemmer.getCurrent();
	}
}
