#ifndef __STRING_UTIL__
#define __STRING_UTIL__

#include <string>
#include <vector>
#include <boost/algorithm/string.hpp>

extern "C" {
#include "stemmer.h"
#include "math.h"
}

using namespace std;

class StringUtil {
  public:

  static vector<string> split(const string &str, const string &delim) {
    vector<string> parts;
    boost::split(parts, str, boost::is_any_of(delim));
    return parts;
  }

  static string toLower(const string &str) {
    string ret = str;
    transform(str.begin(), str.end(), ret.begin(), ::tolower);
    return ret;
  }

  static string trim(const string &str, const string &more = "") {
    string tmp = str;
    string ws = "\t\n\r " + more;
    tmp.erase(tmp.find_last_not_of(ws) + 1);
    tmp.erase(0, tmp.find_first_not_of(ws));
    return tmp;
  }

  static string clean(const string &str, const string &more = "") {
    return trim(toLower(str), more);
  }

  static string stem(const string &word) {
    int len = word.size();
    char *tmp = new char[len + 1];
    tmp[len] = 0;
    for (int i = 0; i < len; ++i) {
      tmp[i] = word[i];
    }
    tmp[pstem(tmp, 0, len-1) + 1] = 0;
    string ret = tmp;
    delete[] tmp;
    return ret;
  }
};

#endif
