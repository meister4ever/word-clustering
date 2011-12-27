import sys
import urllib2
from BeautifulSoup import BeautifulSoup

def find_synonyms(term):
  page = urllib2.urlopen("http://thesaurus.com/browse/%s" % term)
  soup = BeautifulSoup(page)
  if soup(text=lambda(x): 'No results found for' in x):
    return None
  scripts = soup.findAll('script')
  [script.extract() for script in scripts]
  aud_spans = soup.findAll('span', {'class' : 'aud'})
  [aud_span.extract() for aud_span in aud_spans]
  synonyms = []
  sense_cnt = 0
  for content_table in soup('table', {'class' : 'the_content'}):
    rows = content_table.findAll('tr')
    cols = rows[0].findAll('td')
    word = ''.join(cols[1].findAll(text=True)[0:4]).strip()
    main_entry_str = ''.join(cols[0])
    if main_entry_str == 'Main Entry:' and word == term:
      row_idx = 3
      second_row_cols = rows[2].findAll('td')
      if 'Related' in str(second_row_cols[0].findAll(text=True)):
        row_idx = 2
      syn_cols = rows[row_idx].findAll('td')
      syn_str = ''
      for syn in syn_cols[1].findAll(text=True):
        syn_str += str(syn).strip(' \n\r*')
      synonyms = [syn.strip(' \n\r*') for syn in syn_str.split(',')]
      print '%s_%d:%s' % (topic, sense_cnt, ','.join(synonyms))
      sense_cnt += 1
  return synonyms

topic_file = open(sys.argv[1])
for topic in topic_file:
  topic = topic.strip()
  syns = find_synonyms(topic)
topic_file.close()
