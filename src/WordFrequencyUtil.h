#ifndef __WORD_FREQUENCY_UTIL__
#define __WORD_FREQUENCY_UTIL__

#include <map>
#include <string>
#include <vector>

#include "StringUtil.h"
#include "StreamingFileUtil.h"

class WordFrequencyUtil {
 private:
  map<string, int> wordFreq;

 public:
  WordFrequencyUtil(const string &filename) {
    cerr << "Started reading frequency file." << endl;
    StreamingFileUtil fUtil(filename);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, "\t");
      string stemmedWord = StringUtil::stem(parts[0]);
      int freq = atoi(StringUtil::clean(parts[1]).c_str());
      if (wordFreq.count(stemmedWord) > 0) {
        wordFreq[stemmedWord] += freq;
      } else {
        wordFreq[stemmedWord] = freq;
      }
    }
    fUtil.close();
    cerr << "Done reading frequency file." << endl;
  }

  int getFrequency(const string &word) {
    int freq = 0;
    string stemmedWord = StringUtil::stem(word);
    if (wordFreq.count(stemmedWord) > 0) {
      freq = wordFreq[stemmedWord];
    }
    return freq;
  }
};

#endif
