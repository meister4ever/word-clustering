#include <algorithm>
#include <iostream>
#include <map>
#include <set>

#include "StringUtil.h"
#include "StreamingFileUtil.h"
#include "WordFrequencyUtil.h"

unsigned int numWords = 1594793586;

class WordScore {
 public:
  string word;
  double score;

  WordScore(const string &w, const double &s) : word(w), score(s) {}

  bool operator < (const WordScore &rhs) const {
    if (score == rhs.score) {
      return word < rhs.word;
    } else {
      return score > rhs.score;
    }
  }
};

class TopicExpander {
 private:
  WordFrequencyUtil *wfUtil;
  map<string, map<string, double> > adjList;
  map<string, set<WordScore> > clusters, weakClusters;
  double maxFreqThreshold_;
  double minFreqThreshold_;
  double affinityThreshold_;

 public:
  ~TopicExpander() {
    delete wfUtil;
  }

  TopicExpander(string seedWordsFile, string graphFile, string freqFile,
      double edgeThreshold, double minFreqThreshold, double maxFreqThreshold,
      double affinityThreshold, int minConnections, int minEdges) {
    cerr << "Creating Topic expander" << endl;

    maxFreqThreshold_ = maxFreqThreshold;
    minFreqThreshold_ = minFreqThreshold;
    affinityThreshold_ = affinityThreshold;

    wfUtil = new WordFrequencyUtil(freqFile);
    StreamingFileUtil fUtil(graphFile);

    cerr << "Creating graph" << endl;
    unsigned lineCnt = 0;
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, "\t");
      vector<string> words = StringUtil::split(parts[0], ":");
      string word1 = StringUtil::clean(words[0]);
      string word2 = StringUtil::clean(words[1]);
      double score = atof(StringUtil::clean(parts[1]).c_str());
      if (score > edgeThreshold) {
        adjList[word1][word2] = score;
      }
      cerr << lineCnt++ << "\t\r";
    }

    while (removeEdges(minEdges));

    fUtil.close();

    fUtil.open(seedWordsFile);
    unsigned int maxFreq = (unsigned int)(numWords * maxFreqThreshold_);
    unsigned int minFreq = (unsigned int)(numWords * minFreqThreshold_);

    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      vector<string> parts = StringUtil::split(line, ":");
      string topicWord = StringUtil::clean(parts[0]);
      set<WordScore> &clusterWords = clusters[topicWord];
      clusterWords.insert(WordScore(topicWord, 1.0));
      if (parts.size() > 1 && StringUtil::clean(parts[1]).size() > 0) {
        vector<string> otherWords = StringUtil::split(parts[1], ",");
        for (int i = 0; i < otherWords.size(); ++i) {
          clusterWords.insert(WordScore(StringUtil::clean(otherWords[i]), 1.0));
        }
      }
    }
    fUtil.close();

    fUtil.open(freqFile);
    while (!fUtil.done()) {
      string line = fUtil.getNextLine();
      string currentWord = StringUtil::clean(StringUtil::split(line, "\t")[0]);
      set<WordScore> topics = getBestKClustersForWord(currentWord, 10, minConnections, maxFreq);
      for (set<WordScore>::iterator i = topics.begin();
          i != topics.end(); ++i) {
        string topic = i->word;
        clusters[topic].insert(WordScore(currentWord, i->score));
      }
    }

    for (map<string, set<WordScore> >::iterator i = clusters.begin();
        i != clusters.end(); ++i) {
      unsigned int clusterFrequency = getClusterFrequency(clusters[i->first]);
      if (clusterFrequency < minFreq) {
        weakClusters[i->first] = clusters[i->first];
      }
    }

    for (map<string, set<WordScore> >::iterator i = weakClusters.begin();
        i != weakClusters.end(); ++i) {
      clusters.erase(i->first);
    }
  }

  void removeNode(string word) {
    for (map<string, map<string, double> >::iterator i = adjList.begin();
        i != adjList.end(); ++i) {
      if (i->first == word) {
        continue;
      }
      map<string, double> level2Map = i->second;
      for (map<string, double>::iterator j = level2Map.begin();
          j != level2Map.end(); ++j) {
        level2Map.erase(word);
      }
    }
  }

  bool removeEdges(int minEdges) {
    bool removed = false;
    for (map<string, map<string, double> >::iterator i = adjList.begin();
        !removed && i != adjList.end(); ++i) {
      map<string, double> level2Map = i->second;
      string cluster = i->first;
      if (level2Map.size() < minEdges) {
        adjList.erase(cluster);
        removeNode(cluster);
        removed = true;
      }
    }
    return removed;
  }

  unsigned int getClusterFrequency(set<WordScore> clusterWords) {
    unsigned int freq = 0;
    for (set<WordScore>::iterator i = clusterWords.begin();
        i != clusterWords.end(); ++i) {
      freq += wfUtil->getFrequency(i->word);
    }
    return freq;
  }

  bool hasWord(set<WordScore> clusterWords, string word) {
    for (set<WordScore>::iterator i = clusterWords.begin(); 
        i != clusterWords.end(); ++i) {
      if (word == i->word) {
        return true;
      }
    }
    return false;
  }

  set<WordScore> getBestKClustersForWord(string currentWord,
      int maxAllotment, int minConnections, unsigned int maxFreq) {

    set<WordScore> bestClusters;
    for (map<string, set<WordScore> >::iterator i = clusters.begin();
        i != clusters.end(); ++i) {
      string topic = i->first;
      set<WordScore> clusterWords = clusters[topic];
      if (hasWord(clusterWords, currentWord)) {
        continue;
      }
      unsigned int curFreq = getClusterFrequency(clusterWords);
      unsigned int wordFreq = wfUtil->getFrequency(currentWord);

      if (curFreq + wordFreq <= maxFreq) {
        int numConnections = 0;
        double clusterAffinity = getClusterAffinity(currentWord,
            clusterWords, minConnections, numConnections);
        if ((clusterAffinity/numConnections) >= affinityThreshold_) {
          bestClusters.insert(WordScore(topic, clusterAffinity/numConnections));
          if (bestClusters.size() > maxAllotment) {
            bestClusters.erase(*(bestClusters.rbegin()));
          }
        }
      }
    }
    return bestClusters;
  }

  double getClusterAffinity(string currentWord, set<WordScore> clusterWords,
      int minConnections, int &connections) {
    double affinity = 0.0;
    connections = 0;
    if (adjList.count(currentWord) == 0) {
      //cerr << "Didnot find any word in graph for : " << currentWord << endl;
      return affinity;
    }
    map<string, double> level2Map = adjList[currentWord];
    for (set<WordScore>::iterator i = clusterWords.begin();
        i != clusterWords.end(); ++i) {
      double score = 0.0;
      if (level2Map.count(i->word) > 0) {
        score = level2Map[i->word];
        //cerr << i->word << "," << currentWord << "," << score << endl;
      } else {
        continue;
      }
      affinity += score;
      ++connections;
    }

    minConnections = min((size_t)minConnections, clusterWords.size());
      
    if (connections < minConnections) {
      //cerr << "Current word doesnot have minimum connections:(" << connections << "): " << currentWord << endl;
      affinity = 0.0;
    }

    //cerr << currentWord << ", " << clusterWords.begin()->word << " : " << affinity << endl;
   
    return affinity;
  }

  void printCluster(map<string, set<WordScore> > &someClusters) {
    for (map<string, set<WordScore> >::iterator i = someClusters.begin();
        i != someClusters.end(); ++i) {
      cout << i->first << ":";
      bool first = true;
      for (set<WordScore>::iterator j = i->second.begin();
          j != i->second.end(); ++j) {
        //cout << "[" << j->word << "-" << j->score << "]" << ", ";
        if (!first) {
          cout << ",";
        }
        first = false;
        cout << j->word;
      }
      cout << endl << endl;
    }
  }

  void printClusters() {
    printCluster(clusters);
  }

  void printWeakClusters() {
    printCluster(weakClusters);
  }
};

int main(int argc, char *args[]) {
  if (argc < 10) {
    cerr << "Usage: <prog> seedWordsFile graphFile freqFile edgeThresold minFreqThreshold maxFreqThreshold affinityThreshold minConnections minEdges" << endl;
    return -1;
  }
  string seedWordsFile = args[1];
  string graphFile = args[2];
  string freqFile = args[3];
  double edgeThreshold = atof(args[4]);
  double minFreqThreshold = atof(args[5]);
  double maxFreqThreshold = atof(args[6]);
  double affinityThreshold = atof(args[7]);
  int minConnections = atoi(args[8]);
  int minEdges = atoi(args[9]);
  TopicExpander expander(seedWordsFile, graphFile,
      freqFile, edgeThreshold, minFreqThreshold, maxFreqThreshold,
      affinityThreshold, minConnections, minEdges);
  cout << "===================" << endl;
  cout << "Strong Clusters" << endl;
  cout << "===================" << endl;
  expander.printClusters();
  cout << "===================" << endl;
  cout << "Weak Clusters" << endl;
  cout << "===================" << endl;
  expander.printWeakClusters();

  return 0;
}
