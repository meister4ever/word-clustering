import math
import re
import sys
import operator

entity_vectors = {}
total_cnt = 0
input_file = open(sys.argv[1])

for line in input_file:
  line = line.strip()
  e_match = re.match('^([^:]*): (.*)$', line)
  if e_match:
    name, vector_str = e_match.groups()
    entries = vector_str.split(', ')
    entity_vector = {}
    for entry in entries:
      word_idx_str, count_str = entry.split(':')
      word_idx = int(word_idx_str)
      count = int(count_str)
      entity_vector[word_idx] = count
    entity_vectors[name] = entity_vector
input_file.close()

def find_mod(vector):
  vals = vector.values()
  vals.insert(0, 0)
  return math.sqrt(reduce(lambda x,y : x + y**2, vals))

def find_cosine_distance(vector1, vector2):
  mod_vector_1 = find_mod(vector1)
  mod_vector_2 = find_mod(vector2)

  dot_product = 0
  for key, value in vector1.iteritems():
    if key in vector2:
      dot_product += (value * vector2[key])

  return float(dot_product) / (mod_vector_1 * mod_vector_2)

while True:
  cosine_dist = {}
  entities = entity_vectors.keys()
  input_entity = raw_input('Entity: ')
  vector1 = entity_vectors[input_entity]
  for i in range(0, len(entities)):
    if entities[i] == input_entity:
      continue
    vector2 = entity_vectors[entities[i]]
    cosine_dist[entities[i]] = find_cosine_distance(vector1, vector2)

  res_cnt = 0
  for key, value in sorted(cosine_dist.iteritems(), key=operator.itemgetter(1), reverse=True):
    res_cnt += 1
    if res_cnt > 10:
      break
    print key, value
