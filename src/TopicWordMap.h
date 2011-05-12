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
  const WordIdMap &word_id_map_;
  map<string, set<int> > topic_word_map_;

 public:
  TopicWordMap(const string &filename, const WordIdMap &wid_map)
    : word_id_map_(wid_map) {
    StreamingFileUtil fUtil(filename);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, ":");
      string topic = StringUtil::clean(parts[0]);
      vector<string> topic_words = StringUtil::split(parts[1], ",");
      set<int> topic_word_indices;
      for (int i = 0; i < topic_words.size(); ++i) {
        int idx;
        string stemmed_topic_word = StringUtil::stem(topic_words[i]);
        bool ret = word_id_map_.getIdx(stemmed_topic_word, idx);
        if (ret) {
          topic_word_indices.insert(idx);
        }
      }
      topic_word_map_[topic] = topic_word_indices; 
    }
    fUtil.close();
  }

  vector<string> get_topics_for_word(const int &word_idx) const {
    vector<string> topics;
    for (map<string, set<int> >::const_iterator i = topic_word_map_.begin();
        i != topic_word_map_.end(); ++i) {
      if (i->second.count(word_idx) > 0) {
        topics.push_back(i->first);
      }
    }
    return topics;
  }

};

#endif
