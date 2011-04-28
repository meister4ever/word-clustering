LIBS=`ls ../lib/*.jar| tr '\n' ':'`
HADOOP_HOME=/usr/lib/hadoop-0.20
HADOOP_LIB=$HADOOP_HOME/lib/

all:
	echo ${LIBS}
	javac -cp ${LIBS}:${HADOOP_HOME}/hadoop-core.jar:${HADOOP_HOME}/hadoop-tools.jar:${HADOOP_LIB}/commons-cli-1.2.jar:${HADOOP_LIB}/commons-logging-api-1.0.4.jar *.java -d ../bin/
	jar -cvf WordCluster.jar -C ../bin/ .
