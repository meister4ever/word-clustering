import re
import sys

word_dict = {}
total_cnt = 0
input_file = open(sys.argv[1])

for line in input_file:
  #Discounting
  for i in range(1, int(sys.argv[2]) + 1):
    if i not in word_dict:
      word_dict[i] = 0
    word_dict[i] += 1

  line = line.strip()
  e_match = re.match('^([^:]*): (.*)$', line)
  if e_match:
    name, vector_str = e_match.groups()
    entries = vector_str.split(', ')
    for entry in entries:
      word_idx_str, count_str = entry.split(':')
      word_idx = int(word_idx_str)
      count = int(count_str)

      if word_idx not in word_dict:
        word_dict[word_idx] = 0

      word_dict[word_idx] += count
      total_cnt += count

input_file.close()

for word, count in word_dict.iteritems():
  word_dict[word] = float(word_dict[word]) / total_cnt

for word in sorted(word_dict.keys()):
  print word, word_dict[word]
