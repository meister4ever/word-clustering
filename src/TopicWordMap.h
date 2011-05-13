#ifndef __TOPIC_WORD_MAP__
#define __TOPIC_WORD_MAP__

#include <iostream>
#include <map>
#include <set>
#include <string>
#include <vector>

#include "WordIdMap.h"
#include "StreamingFileUtil.h"
#include "StringUtil.h"

class TopicWordMap {
 private:
  const WordIdMap &wordIdMap_;
  map<string, set<int> > topicWordMap_;

 public:
  TopicWordMap(const string &filename, const WordIdMap &widMap)
    : wordIdMap_(widMap) {
    StreamingFileUtil fUtil(filename);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, ":");
      string topic = StringUtil::clean(parts[0]);
      vector<string> topicWords = StringUtil::split(parts[1], ",");
      set<int> topicWordIndices;
      for (int i = 0; i < topicWords.size(); ++i) {
        int idx;
        string stemmedTopicWord = StringUtil::stem(topicWords[i]);
        bool ret = wordIdMap_.getIdx(stemmedTopicWord, idx);
        if (ret) {
          topicWordIndices.insert(idx);
        }
      }
      topicWordMap_[topic] = topicWordIndices; 
    }
    fUtil.close();
  }

  vector<string> getTopicsForWord(const int &wordIdx) const {
    vector<string> topics;
    for (map<string, set<int> >::const_iterator i = topicWordMap_.begin();
        i != topicWordMap_.end(); ++i) {
      if (i->second.count(wordIdx) > 0) {
        topics.push_back(i->first);
      }
    }
    return topics;
  }

  vector<string> getAllTopics() {
    vector<string> topics;
    for (map<string, set<int> >::const_iterator i = topicWordMap_.begin();
        i != topicWordMap_.end(); ++i) {
      topics.push_back(i->first);
    }
    return topics;
  }

};

#endif
