#set -xv

hadoop fs -rmr /user/rohith/wikicoccur

hadoop jar WordCluster.jar WikiWordCooccurrence wiki/ /user/rohith/wikicoccur/ 2 500 "data/top10KWords.stemmed.txt" 10
#hadoop jar WordCluster.jar WikiWordCooccurrence wiki/ wikicoccur/ 3 30
