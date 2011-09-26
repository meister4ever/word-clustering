import re
import sys

MIN_SYNS = 10


# Utility function to add a key, value to a dictionary
def add_value(key, value, dictionary):
  if not key in dictionary:
    dictionary[key] = []
  if value not in dictionary[key]:
    dictionary[key].append(value)



# Utility function to add a key, list_value to a dictionary
def add_list_value(key, list_value, dictionary):
  if not key in dictionary:
    dictionary[key] = []
  for value in list_value:
    if value not in dictionary[key]:
      dictionary[key].append(value)



# Regex that matches the synonym lines.
syn_match = re.compile('^\((\w+)\)\|([\w\|\-\' ]+)$')

# The logic of this function is that we consider only those words which
# have majority of their synonyms as adjectives. We also threshold the
# words by forcing them to have atleast 'k' synonyms.
def add_if_trait(trait_name, trait_syn_size, thesaurus_file, trait_dict):
  line_cnt = 0
  adj_cnt = 0
  total_syns = 0
  all_syns = []

  for line in thesaurus_file:
    line = line.strip()
    match = syn_match.match(line)

    if match:
      # Get the pos and the syns under that pos
      pos, syn_str_for_pos = match.groups()
      syns_for_pos = syn_str_for_pos.split('|')

      # Check for adj, we have a threshold based on the number of
      # adjective synonyms.
      if pos == 'adj':
        adj_cnt += len(syns_for_pos)

      total_syns += len(syns_for_pos)

      # Update the syns list to hold the synonyms.
      for syn in syns_for_pos:
        if syn not in all_syns:
          all_syns.append(syn.lower()) 

    line_cnt += 1
    if line_cnt >= int(trait_syn_size):
      break

  # Check for the conditions to treat the word as a trait.
  if adj_cnt >= total_syns/2 and total_syns > MIN_SYNS:
    print '%s\t%s' % (trait_name.lower(), '\t'.join(all_syns))
    #add_list_value(trait_name, all_syns, trait_dict)
     



# Function that runs through the thesaurus file, calling add_if_trait for
# each word found.  
def parse_thesaurus_file(thesaurus_file):
  trait_dict = {}

  # Regex to match the word entry
  word_match = re.compile('(^[a-zA-Z\-\']+)\|(\d+)$')

  for line in thesaurus_file:
    # If the line has a word with number, for eg: word1|8, it is a word entry
    line = line.strip()
    match = word_match.match(line)

    if match:
      trait_name, trait_syn_size = match.groups()
      # Add if our heuristics tell that it is a trait.
      add_if_trait(trait_name,
          trait_syn_size,
          thesaurus_file,
          trait_dict)

  return trait_dict



def main():
  if len(sys.argv) < 2:
    usage()
    return
  
  # Parse the thesaurus file. 
  thesaurus_file = open(sys.argv[1])
  trait_dict = parse_thesaurus_file(thesaurus_file)
  thesaurus_file.close()

  # Print out the traits we filtered from the thesaurus file.
  for word, trait_list in trait_dict.items():
    print '%s => %s' % (word, str(trait_list))

main()
