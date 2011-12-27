#hadoop fs -rmr /user/rohith/lydia_word_graph
#hadoop jar WordCluster.jar MRCreateWordGraph lydia_mutual_info/part-* /user/rohith/lydia_word_graph/ 500 "/tmp/wordGraphCounterFile.txt"
hadoop fs -rmr lydia_2hop
hadoop jar WordCluster.jar MRWordCrossSimilarity lydia_word_graph/part-* lydia_2hop "/tmp/wordGraphCounterFile.txt" 500
