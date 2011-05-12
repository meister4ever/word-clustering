USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wordnet_cooccur_unstemmed

hadoop jar WordCluster.jar WordnetCooccurence /user/rmenon/word_clustering/data/newTop10KWords.unstemmed.txt wordnet_cooccur_unstemmed/ 48 "${HPATH}/data/newTop10KWords.unstemmed.txt" 3
