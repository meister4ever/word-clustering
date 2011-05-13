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
  map<int, string> indexWordMap_; 
  map<string, int> wordIndexMap_; 

 public:
  WordIdMap(const string &filename) {
    StreamingFileUtil fUtil(filename);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, "\t");
      string word = StringUtil::clean(parts[0]);
      string stemmedWord = StringUtil::stem(parts[0]);
      int idx = atoi(StringUtil::clean(parts[1]).c_str());
      indexWordMap_[idx] = word;
      wordIndexMap_[stemmedWord] = idx;
    }
    fUtil.close();
  }

  bool getWord(const int &idx, string &str) const {
    map<int, string>::const_iterator it = indexWordMap_.find(idx);
    if (it == indexWordMap_.end()) {
      return false;
    } else {
      str = it->second;
      return true;
    }
  }

  bool getIdx(const string &word, int &idx) const {
    string stemmedWord = StringUtil::stem(word);
    map<string, int>::const_iterator it = wordIndexMap_.find(stemmedWord);
    if (it == wordIndexMap_.end()) {
      return false;
    } else {
      idx = it->second;
      return true;
    }
  }

  int getNumWords() {
    return indexWordMap_.size();
  }
};

#endif
