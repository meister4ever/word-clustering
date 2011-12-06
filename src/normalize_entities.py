from mpmath import *
import math
import sys
import xalglib

import numpy as np
import matplotlib as mpl
mpl.use('pdf')

import numpy as np
import pylab as P

class Normalizer():

  def normal_estimate(self, s, p, n):
    u = n * p
    o = power(u * (1-p), 0.5)
    val = 0.5 * (1.0 + erf((s-u)/power(o*2, 0.5)))
    return val

  def __init__(self, word_dictionary, entity_vectors, categories):

    # Create a dictionary from the word dictionary file.
    self.word_dict = self.parse_dictionary(word_dictionary)
    # Parse the categories.
    self.categories = self.parse_categories(categories)
    # Create a word count dictionary from the entity vectors.
    self.category_count, self.total_count = self.parse_entity_vectors(entity_vectors)

    self.category_entity_dict = {}

    mp.dps = 10

    entity_vectors_file = open(entity_vectors)
    for line in entity_vectors_file:
      line = line.strip()
      parts = line.split(', ')
      entity_name = parts[0].split(':')[0]
      first_word = parts[0].strip(entity_name + ': ')
      parts[0] = first_word

      entity_word_cnt_dict = {}
      entity_total_words = 0
      for word in parts:
        id, cnt = word.split(':')
        word = self.word_dict[id]
        if word not in self.categories:
          continue
        categories = self.categories[word]
        for category in categories:
          if category not in entity_word_cnt_dict:
            entity_word_cnt_dict[category] = 0
          entity_word_cnt_dict[category] += int(cnt)
          entity_total_words += int(cnt)

      #scale_factor = 1000000.0 / float(entity_total_words + 1.0)
      #for category, cnt in entity_word_cnt_dict.iteritems():
      #  entity_word_cnt_dict[category] = int(cnt * scale_factor) 

      #entity_total_words = 1000000

      probability_values = []
      for category, cnt in entity_word_cnt_dict.iteritems():
        if cnt == 0:
          continue
        exp_cnt = entity_total_words * float(self.category_count[category]) / float(self.total_count)
        cat_prob = float(self.category_count[category]) / float(self.total_count)
        #if cnt < exp_cnt:
        #  cnt = exp_cnt
        #cur_prob = self.binomial(entity_total_words, cnt, cat_prob)
        #if cur_prob >= 1.0:
        #  cur_prob = mpf(100000000.0) / mpf(100000001.0)
        cur_prob = mpf(cnt + 1.0) / mpf(exp_cnt + 1.0)
        probability_values.append((category, cur_prob, self.category_count[category], self.total_count, cnt, entity_total_words))
        if category not in self.category_entity_dict:
          self.category_entity_dict[category] = []
        self.category_entity_dict[category].append((entity_name, cur_prob))

      probability_values = sorted(probability_values, key=lambda probability_value: probability_value[1], reverse=True)
      print '%s\t%s' % (entity_name, str(probability_values[0:3]))

    entity_vectors_file.close()
    print '\n\n==========================================\n\n'
    for category, entity_list in self.category_entity_dict.iteritems():
      tmp_list = sorted(entity_list, key = lambda pv: pv[1], reverse=True) 
      print category
      #print 'Top: %s\n' % str(tmp_list[:3])
      print 'Top: %s\n' % str([x[0] for x in tmp_list])
      #print 'Bottom: %s\n' % str(tmp_list[len(tmp_list)-4:])
      # the histogram of the data with histtype='step'
      x = [val[1] for val in tmp_list]
      n, bins, patches = P.hist(x, 50, normed=1, histtype='stepfilled')
      #P.setp(patches, 'facecolor', 'g', 'alpha', 0.75)
      P.savefig('/tmp/plot/%s.pdf' % category)
      P.clf()

  def binomial(self, n, k, p):
    #mpf_1 = mpf(1.0)
    #return mpf_1 - betainc(n - k - 1, k, 0, 1-p, True)
    total_prob = mpf(0.0)
    i = k
    while i <= n:
      total_prob = total_prob + binomial(n,i) * power(p, i) * power(1-p, n-i)
      return total_prob
      i = i + 1
    return total_prob

  def parse_dictionary(self, word_dictionary):
    word_dict = {}
    word_dict_file = open(word_dictionary)
    for line in word_dict_file:
      line = line.strip()
      id, word = line.split(' ')
      word_dict[id] = word
    word_dict_file.close()
    return word_dict

  def parse_categories(self, categories):
    category_dict = {}
    category_file = open(categories)
    for line in category_file:
      line = line.strip()
      category, seedword_str = line.split(':')
      for seed_word in seedword_str.split(','):
        if seed_word not in category_dict:
          category_dict[seed_word] = []
        category_dict[seed_word].append(category)
    category_file.close()
    return category_dict

  def parse_entity_vectors(self, entity_vectors):
    word_cnt_dict = {}
    total_words = 0
    entity_vectors_file = open(entity_vectors)
    for line in entity_vectors_file:
      line = line.strip()
      parts = line.split(', ')
      entity_name = parts[0].split(':')[0]
      first_word = parts[0].strip(entity_name + ': ')
      parts[0] = first_word

      entity_total_words = 0
      for word in parts:
        id, cnt = word.split(':')
        word = self.word_dict[id]
        if word not in self.categories:
          continue
        categories = self.categories[word]
        for category in categories:
          if category not in word_cnt_dict:
            word_cnt_dict[category] = 0
          word_cnt_dict[category] += int(cnt)
          total_words += int(cnt)

    entity_vectors_file.close()
    return word_cnt_dict, total_words 

normalizer = Normalizer(sys.argv[1], sys.argv[2], sys.argv[3])
