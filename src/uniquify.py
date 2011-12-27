import sys

infile = open(sys.argv[1])
for line in infile:
  line = line.strip()
  category, word_str = line.split(':')
  words = word_str.split(',')
  unique_words = []
  for word in words:
    if word not in unique_words:
      unique_words.append(word)
  print '%s:%s' % (category, ','.join(unique_words))
infile.close()
