LIBS=`ls ../lib/*.jar| tr '\n' ':'`
HADOOP_HOME=/home/hadoop4/hadoop_install/hadoop
HADOOP_LIB=${HADOOP_HOME}/lib/

all:
	echo ${LIBS}
	javac -cp ${HADOOP_HOME}/hadoop-0.20.0-lydia-core.jar:${HADOOP_HOME}/hadoop-0.20.0-lydia-tools.jar:${HADOOP_HOME}/lib/commons-logging-1.0.4.jar:${HADOOP_HOME}/lib/commons-logging-api-1.0.4.jar:${HADOOP_HOME}/lib/commons-cli-2.0-SNAPSHOT.jar:${LIBS} *.java -d ../bin/
	jar -cvf WordCluster.jar -C ../bin/ .
