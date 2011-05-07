USERNAME=`whoami`
HDFS_ADDR="mshadoop1"
HPATH="/user/$USERNAME/word_clustering/"

hadoop fs -rmr wordnet_cooccur_stemmed

hadoop jar WordCluster.jar WordnetCooccurence /user/rmenon/word_clustering/data/top10KWords.stemmed.txt wordnet_cooccur_stemmed/ 48 "${HPATH}/data/top10KWords.stemmed.txt" 3
