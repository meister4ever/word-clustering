#set -xv

USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wikiout

hadoop jar WordCluster.jar WikiWordCount wiki/ wikiout/ 3 48 "${HPATH}/data/stopWords.txt" "hdfs://${HDFS_ADDR}${HPATH}/data/postag.model#postag_model"
