import random
import sys

f = open(sys.argv[1])
word_cnt = {}
for line in f:
  if random.random() * 100 > 10:
    continue
  line = line.strip().lower().replace('tiger woods', 'tigerwoods')
  words = line.split(' ')
  for word in words:
    word = word.strip(',.?! \n\r\'"`@#$%^&*()+:;*-+_)')
    if word != 'tigerwoods' or word != 'woods':
      if word not in word_cnt:
        word_cnt[word] = 0
      word_cnt[word] = word_cnt[word] + 1
f.close()
for key, value in word_cnt.iteritems():
  print key, value
