#ifndef __WORDID_MAP__
#define __WORDID_MAP__

#include <iostream>
#include <map>
#include <string>
#include <vector>

#include "StreamingFileUtil.h"
#include "StringUtil.h"

using namespace std;

class WordIdMap {
 private:
  map<int, string> index_word_map_; 
  map<string, int> word_index_map_; 

 public:
  WordIdMap(const string &filename) {
    StreamingFileUtil fUtil(filename);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, "\t");
      string word = StringUtil::clean(parts[0]);
      string stemmed_word = StringUtil::stem(parts[0]);
      int idx = atoi(StringUtil::clean(parts[1]).c_str());
      index_word_map_[idx] = word;
      word_index_map_[stemmed_word] = idx;
    }
    fUtil.close();
  }

  bool getWord(const int &idx, string &str) const {
    map<int, string>::const_iterator it = index_word_map_.find(idx);
    if (it == index_word_map_.end()) {
      return false;
    } else {
      str = it->second;
      return true;
    }
  }

  bool getIdx(const string &word, int &idx) const {
    string stemmed_word = StringUtil::stem(word);
    map<string, int>::const_iterator it = word_index_map_.find(stemmed_word);
    if (it == word_index_map_.end()) {
      return false;
    } else {
      idx = it->second;
      return true;
    }
  }
};

#endif
