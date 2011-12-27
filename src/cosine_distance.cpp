#include <cstdlib>
#include <cmath>
#include <iostream>
#include <fstream>
#include <map>
#include <ext/hash_map>

using namespace std;
using namespace __gnu_cxx;

namespace __gnu_cxx {
#ifndef __HASH_STRING__
#define __HASH_STRING__
  template <>
    struct hash<string> {
      size_t operator() (const std::string& s) const
      {
        size_t h = 0;
        std::string::const_iterator p, p_end;
        for(p = s.begin(), p_end = s.end(); p != p_end; ++p)
        {
          h = 31 * h + (*p);
        }
        return h;
      }
    };
#endif
};


typedef hash_map<int, double> ev_t;
typedef hash_map<string, ev_t> evs_t;
typedef hash_map<string, double> cd_t;

double find_mod(const ev_t &vec) {
  double sum_of_squares = 0;
  for (ev_t::const_iterator i = vec.begin(); i != vec.end(); ++i) {
    sum_of_squares += (i->second * i->second);
  }
  return sqrt(sum_of_squares);
}

double find_cosine_distance(const ev_t &v1, const ev_t &v2) {
  double mod_v1 = find_mod(v1);
  double mod_v2 = find_mod(v2);

  double dot_product = 0;
  for (ev_t::const_iterator i = v1.begin(); i != v1.end(); ++i) {
    if (v2.count(i->first)) {
      dot_product += (i->second * v2.find(i->first)->second);
    }
  }
  return dot_product / (mod_v1 * mod_v2); 
}

void add_entity(evs_t &entity_vectors, const string &line) {
  int pos = line.find(":");
  string name = line.substr(0, pos);
  string rest = line.substr(pos + 2);
  ev_t entity_vector;
  while (true) {
    pos = rest.find(",");
    string val = rest;
    if (pos != string::npos) {
      val = rest.substr(0, pos);
    }
    rest = rest.substr(pos + 2);
    int pos_colon = val.find(":");
    int word_idx = atoi(val.substr(0, pos_colon).c_str());
    int count = atoi(val.substr(pos_colon + 1).c_str());
    entity_vector[word_idx] = count;
    if (pos == string::npos) {
      break;
    }
  }
  entity_vectors[name] = entity_vector;
}

int main(int argc, char *argv[]) {
  evs_t entity_vectors; 
  char *line = new char[540000];
  int l = 0;
  ifstream ifs(argv[1]);
  while (ifs.getline(line, 540000)) {
    add_entity(entity_vectors, line);
    cout << l++ << endl;
  }
  delete line;
  ifs.close();

  cd_t cos_distance;
  int k = 0;
  for (evs_t::const_iterator i = entity_vectors.begin();
      i != entity_vectors.end(); ++i) {
    cout << k << endl;
    for (evs_t::const_iterator j = i; j != entity_vectors.end();
        ++j) {
      if (i != j) {
        string key = i->first + "" + j->first;
        float value = find_cosine_distance(i->second, j->second);
        cos_distance[key] = value;
      }
    }
    ++k;
  }

  for (cd_t::const_iterator i = cos_distance.begin();
  i != cos_distance.end(); ++i) {
    cout << i->first << "\t" << i->second << endl;
  }

  return 0;
}
