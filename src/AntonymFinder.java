import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import net.didion.jwnl.JWNLException;


public class AntonymFinder {
	public static void main(String[] args) throws JWNLException, FileNotFoundException {
		String fileContent = FileUtil.ReadFileContent(args[0]);
		WordnetUtil wnUtil = WordnetUtil.getInstance();
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split(":");
			String topic = StringUtil.clean(parts[0]);
			HashSet<Integer> topicWordIndices = new HashSet<Integer>();
			
			StringBuilder sb = new StringBuilder();
			sb.append(topic + ":");
			String[] topicWordStrings = parts[1].split(",");
			int numWords = 0;
			boolean first = true;
			
			for (String topicWordString : topicWordStrings) {
				Set<String> antonyms = wnUtil.getAntonyms(StringUtil.clean(topicWordString));
				for (String antonym : antonyms) {
					if (first) {
						sb.append(",");
					}
					sb.append(antonym);
					++numWords;
				}
			}
			if (numWords > 0) {
				System.out.println(sb.toString());
			}
		}
	}
}
