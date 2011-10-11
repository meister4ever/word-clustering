import sqlite3
import sys

class Dumper:
  def __init__(self, connection):
    self.connection = connection
    self.cursor = connection.cursor()

  def CreateEntityVectorsTable(self, columns):
    self.topics = columns
    self.cursor.execute('drop table if exists entity_vectors')
    self.connection.commit()

    create_query = 'create table entity_vectors ('
    create_query += 'entity_name text'
    for column in columns:
      create_query += ', "%s" real' % column
    create_query += ')'
    #print 'Create query: %s' % create_query
    self.cursor.execute(create_query)
    self.connection.commit()

  def InsertEntityVectors(self, filename):
    file = open(filename, 'r')
    table_created = 0
    record_cnt = 0
    for line in file:
      topic_dict = {}
      entity, entity_vector = line.strip().split('\t')
      topicScores = entity_vector.split(',')
      for topicScore in topicScores:
        topic, score = topicScore.split(':')
        topic_dict[topic] = score;
  
      sorted_topics = topic_dict.keys()
      sorted_topics.sort()
      if table_created == 0:
        self.CreateEntityVectorsTable(sorted_topics)
        table_created = 1
  
      insert_query = 'insert into entity_vectors values("%s"' % entity
      for topic in sorted_topics:
        insert_query += ', %s' % topic_dict[topic]
      insert_query += ')'
  
      #print 'Insert query: %s' % insert_query
      self.cursor.execute(insert_query)
      self.connection.commit()
      record_cnt += 1
      #print record_cnt

  def DumpTopKEntitiesForTopics(self, num_entities):
    topic_number = 1
    for topic in self.topics:
      self.cursor.execute('''select entity_name from entity_vectors
        order by "%s" desc limit %d''' % (topic, num_entities))
      print '%d] Topic %s' % (topic_number, topic)
      print '\tTop:'
      entity_num = 1
      for row in self.cursor.fetchall():
        print '\t%d] %s' % (entity_num, row[0])
        entity_num += 1
      print '\n'

      self.cursor.execute('''select entity_name from entity_vectors
        order by "%s" asc limit %d''' % (topic, num_entities))
      print '\tBottom:'
      entity_num = 1
      for row in self.cursor.fetchall():
        print '\t%d] %s' % (entity_num, row[0])
        entity_num += 1
      print '\n'

      topic_number += 1

connection = sqlite3.connect(sys.argv[1])
dumper = Dumper(connection)
dumper.InsertEntityVectors(sys.argv[2])
dumper.DumpTopKEntitiesForTopics(5)
connection.commit()
connection.close()
