#hadoop fs -rmr /user/rohith/lydia_coccur
#hadoop jar WordCluster.jar LydiaWordCooccurrence lydia/lydia.2011.data /user/rohith/lydia_coccur/ 2 500 "data/eng-dict.txt" 25 "/tmp/counterFile.txt"
#hadoop fs -rmr lydia_cond_prob;hadoop jar WordCluster.jar CondProbability lydia_coccur/part-* lydia_cond_prob 500
hadoop fs -rmr lydia_mutual_info;hadoop jar WordCluster.jar MutualInformation lydia_cond_prob/part-* lydia_mutual_info "/tmp/counterFile.txt" 500
