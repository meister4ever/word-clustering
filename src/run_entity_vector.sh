hadoop fs -rmr /user/rohith/lydia_entity_vector
hadoop jar WordCluster.jar LydiaEntityVectorCreator lydia/lydia.2011.data /user/rohith/lydia_entity_vector 2 500 "data/eng-dict.txt" 25 "data/entityList.txt"
