#ifndef __STREAMING_FILE_UTIL__
#define __STREAMING_FILE_UTIL__

#include <iostream>
#include <fstream>

using namespace std;

class StreamingFileUtil {
 private:
  ifstream ifs;
  string currentLine;
  char *buf;

 public:
  StreamingFileUtil(const string &filename) {
    cerr << "Opening file : " << filename << endl;
    buf = new char[10485760];
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
    ifs.getline(buf, 10485759);
    return buf;
  }

  string getNextLine() {
    return currentLine;
  }

  void close() {
    delete[] buf;
    ifs.close();
  }
};

#endif
