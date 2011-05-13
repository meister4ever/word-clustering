make
hadoop fs -rmr /user/rohith/entity_similarity/topic_repr
hadoop jar WordCluster.jar MRWordTopicConverter /user/rohith/entity_similarity/input/entities_all_10k /user/rohith/entity_similarity/topic_repr
