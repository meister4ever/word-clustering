LIBS=`ls ../lib/*.jar| tr '\n' ':'`
HADOOP_HOME=/home/hadoop_newton/hadoop_install/
HADOOP_LIB=${HADOOP_HOME}/lib/

all:
	echo ${LIBS}
	rm -rf ../bin/*
	javac -cp ${HADOOP_HOME}/hadoop-core-0.20.203.0.jar:${HADOOP_HOME}/hadoop-tools-0.20.203.0.jar:${HADOOP_HOME}/lib/commons-logging-1.0.4.jar:${HADOOP_HOME}/lib/commons-logging-api-1.0.4.jar:${HADOOP_HOME}/lib/commons-cli-2.0-SNAPSHOT.jar:${LIBS} *.java -d ../bin/
	jar -cvf WordCluster.jar -C ../bin/ .
