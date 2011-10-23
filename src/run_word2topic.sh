make
WORKING_DIR="/scratch/rohith/entity_similarity/"

# Word to topic conversion.
if [ ! -e ${WORKING_DIR}/topic_repr/mr.done ]; then
  echo "Running word to topic converstion.."
  hadoop fs -rmr /user/rohith/entity_similarity/topic_repr
  #hadoop jar WordCluster.jar MRWordTopicConverter /user/rohith/entity_similarity/input/entities_all_10k /user/rohith/entity_similarity/topic_repr
  hadoop jar WordCluster.jar MRWordTopicConverter /user/rohith/entity_similarity/input/entities.test /user/rohith/entity_similarity/topic_repr
  rm -rf ${WORKING_DIR}/topic_repr/part-*
  hadoop fs -get /user/rohith/entity_similarity/topic_repr ${WORKING_DIR}/topic_repr
  touch ${WORKING_DIR}/topic_repr/mr.done
fi

# Stat generation.
if [ ! -e ${WORKING_DIR}/topic_repr_stats/mr.done ]; then
  echo "Running topic stat generation.."
  hadoop fs -rmr /user/rohith/entity_similarity/topic_repr_stats
  hadoop jar WordCluster.jar MRTopicStatistics /user/rohith/entity_similarity/topic_repr/part* /user/rohith/entity_similarity/topic_repr_stats 35
  rm -rf ${WORKING_DIR}/topic_repr_stats/part-*
  hadoop fs -get /user/rohith/entity_similarity/topic_repr_stats ${WORKING_DIR}/topic_repr_stats
  cat ${WORKING_DIR}/topic_repr_stats/part-* > ${WORKING_DIR}/topic_repr_stats/topicStats.txt
  touch ${WORKING_DIR}/topic_repr_stats/mr.done
fi

# Normalization.
if [ ! -e ${WORKING_DIR}/topic_repr_normalized/mr.done ]; then
  echo "Running normalization.."
  hadoop fs -rmr /user/rohith/entity_similarity/topic_repr_normalized
  hadoop fs -rm /user/rohith/entity_similarity/topic_repr_stats/topicStats.txt
  hadoop fs -copyFromLocal ${WORKING_DIR}/topic_repr_stats/topicStats.txt /user/rohith/entity_similarity/topic_repr_stats/topicStats.txt
  hadoop jar WordCluster.jar MRNormalizeEntityTopic /user/rohith/entity_similarity/topic_repr/part* /user/rohith/entity_similarity/topic_repr_normalized /user/rohith/entity_similarity/topic_repr_stats/topicStats.txt
  rm -rf ${WORKING_DIR}/topic_repr_normalized/part-*
  hadoop fs -get /user/rohith/entity_similarity/topic_repr_normalized ${WORKING_DIR}/topic_repr_normalized
  cat ${WORKING_DIR}/topic_repr_normalized/part-* > ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector.txt
  head -1000 ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector.txt | \
  awk 'BEGIN{ln=1;}{printf("%d%s\n",ln,$0);ln++;}' > \
  ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln.txt
  NUM_LINES=`wc -l ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln.txt | awk '{print $1;}'`
  NUM_LINES=$((NUM_LINES/1))
  split -l $NUM_LINES ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln.txt \
  ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln_
  hadoop fs -rm /user/rohith/entity_similarity/topic_repr_normalized/normalized_topic_entity_vector_with_ln_*.txt
  hadoop fs -copyFromLocal ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln_* \
  /user/rohith/entity_similarity/topic_repr_normalized/
  touch ${WORKING_DIR}/topic_repr_normalized/mr.done
fi

if [ ! -e ${WORKING_DIR}/entity_cross_similarity/mr.done ]; then
  echo "Running Cross similarity.."
  hadoop fs -rmr /user/rohith/entity_similarity/entity_cross_similarity
  NUM_ENTITIES=`cat ${WORKING_DIR}/topic_repr_normalized/normalized_topic_entity_vector_with_ln.txt | wc -l`
  hadoop jar WordCluster.jar MREntityCrossSimilarity /user/rohith/entity_similarity/topic_repr_normalized/normalized_topic_entity_vector_with_ln_* /user/rohith/entity_similarity/entity_cross_similarity $NUM_ENTITIES 35
  hadoop fs -get /user/rohith/entity_similarity/entity_cross_similarity ${WORKING_DIR}/entity_cross_similarity
  cat ${WORKING_DIR}/entity_cross_similarity/part-* | sort -t '	' -k 2 -nr > ${WORKING_DIR}/entity_cross_similarity/entity_cross_similarity.txt
  touch ${WORKING_DIR}/entity_cross_similarity/mr.done
fi
