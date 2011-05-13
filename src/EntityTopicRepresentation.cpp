

#include "TopicWordMap.h"
#include "WordIdMap.h"

int main(int argc, char *argv[]) {
  if (argc < 4) {
    cerr << "Usage : " << argv[0] << " <word-id-map> <topic-word-file> <entity-vector-file>" << endl;
    return -1;
  }

  WordIdMap wordIdMap(argv[1]);
  TopicWordMap topicWordMap(argv[2], wordIdMap);
  StreamingFileUtil fUtil(argv[3]);

  int numWords = wordIdMap.getNumWords();
  vector<string> allTopics = topicWordMap.getAllTopics();

  int lineCnt = 0;
  while (!fUtil.done()) {
    map<int, double> freqMap;
    map<string, double> topicFreqMap;
    int totalWordFreq = 0;

    // Initialize to 1 for discounting.
    totalWordFreq = numWords;
    for (int i = 1; i <= numWords; ++i) {
      freqMap[i] = 1;
    }

    string line = fUtil.getNextLine();
    cerr << ++lineCnt << "\t\r";
    int pos = line.find(":");
    string entityName = line.substr(0, pos);
    string wordVectorStr = line.substr(pos + 1); 
    vector<string> parts = StringUtil::split(wordVectorStr, ",");
    for (int i = 0; i < parts.size(); ++i) {
      vector<string> wordParts = StringUtil::split(parts[i], ":");
      int wordIdx = atoi(StringUtil::clean(wordParts[0]).c_str());
      int wordFreq = atoi(StringUtil::clean(wordParts[1]).c_str());
      freqMap[wordIdx] += wordFreq;
      totalWordFreq += wordFreq;
    }

    for (int i = 0; i < allTopics.size(); ++i) {
      topicFreqMap[allTopics[i]] = 1.0/(double)totalWordFreq;
    }

    for (int i = 1; i <= numWords; ++i) {
      vector<string> topics = topicWordMap.getTopicsForWord(i);
      double wordProb = (double)freqMap[i]/(double)totalWordFreq;
      for (int j = 0; j < topics.size(); ++j) {
        topicFreqMap[topics[j]] += wordProb; 
      }
    }

    cout << entityName << "\t";
    for (int i = 0; i < allTopics.size(); ++i) {
      if (i != 0) {
        cout << ",";
      }
      cout << allTopics[i] << ":" << topicFreqMap[allTopics[i]];
    }
    cout << endl;
  }

  fUtil.close();

  return 0;
}
