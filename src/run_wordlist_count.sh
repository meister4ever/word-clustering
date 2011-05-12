#set -xv

USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wiki_wlc

hadoop jar WordCluster.jar WikiWordListCounter wiki/ wiki_wlc/ 48 "${HPATH}/data/wordlist.txt"
