#set -xv

USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wikicoccur

hadoop jar WordCluster.jar WikiWordCooccurrence wiki/ wikicoccur/ 3 48 "${HPATH}/data/newTop10KWords.unstemmed.txt"
