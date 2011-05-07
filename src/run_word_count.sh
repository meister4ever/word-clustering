#set -xv

USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wiki_wc

hadoop jar WordCluster.jar WikiWordCount wiki/ wiki_wc/ 3 48 "${HPATH}/data/stopWords.txt" "hdfs://${HDFS_ADDR}${HPATH}/data/postag.model#postag_model"
