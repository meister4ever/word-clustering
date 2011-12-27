public class WordTopicConverterTest {
  public static void main(String args[]) {
    StopwordUtil stopwordUtil = new StopwordUtil("/home/rohith/word-clustering/data/stopWords.txt");
    WordIdMap wordIdMap = new WordIdMap("/home/rohith/word-clustering/data/entity_dict.txt", "/home/rohith/word-clustering/data/word_prob.txt", stopwordUtil);
    TopicWordMap topicWordMap = new TopicWordMap("/home/rohith/word-clustering/data/thesaurus.crawl.txt", wordIdMap);
    WordTopicConverter wordTopicConverter = new WordTopicConverter(wordIdMap, topicWordMap);
    String fileContent = FileUtil.ReadFileContent(args[0]);
    wordTopicConverter.convertWords2Topics(fileContent);
  }
}
