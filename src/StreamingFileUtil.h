#ifndef __STREAMING_FILE_UTIL__
#define __STREAMING_FILE_UTIL__

#include <iostream>
#include <fstream>

using namespace std;

class StreamingFileUtil {
 private:
  ifstream ifs;
  string currentLine;

 public:
  StreamingFileUtil(const string &filename) {
    cerr << "Opening file : " << filename << endl;
    ifs.open(filename.c_str());
  }

  void open(const string &filename) {
    ifs.open(filename.c_str());
  }

  bool done() {
    currentLine = getNextLineI();
    return ifs.eof();
  }

  string getNextLineI() {
    char buf[8192];
    ifs.getline(buf, 8191);
    return buf;
  }

  string getNextLine() {
    return currentLine;
  }

  void close() {
    ifs.close();
  }
};

#endif
