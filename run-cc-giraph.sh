#!/bin/bash

# stop on any error
set -e

pushd `dirname $0` > /dev/null
PROJECT_HOME=`pwd -P`
popd > /dev/null

if [ -z "$1" ]
then
	echo "Usage: ./run-cc-giraph.sh local/path/to/input/with/graph, e.g. ./run-cc-giraph.sh download/small.txt"
	exit 1
fi

INPUT_DATA="$1"

# in my case HADOOP_HOME=/opt/hadoop/hadoop-2.8.2
if [ -z "$HADOOP_HOME" ]
then
    echo "set HADOOP_HOME variable to point your hadoop installation, e.g.: export HADOOP_HOME=/opt/hadoop/hadoop-2.8.2"
    exit 1
fi

INPUT_HDFS="/user/wos_michal/input"
OUTPUT_HDFS="/user/wos_michal/output"

pushd $PROJECT_HOME/giraph-cc
    export MAVEN_OPTS='-Xms384M -Xmx512M -XX:MaxPermSize=256M'
    mvn -Phadoop_2 -Dhadoop.version=2.8.2 -DskipTests clean package || { echo "Error while building"; exit 1; }
popd

#INIT DATA    
$HADOOP_HOME/bin/hdfs dfs -mkdir -p /user/wos_michal/ || { echo "Error while creating output dir in hdfs"; exit 1; }
# overwrites if exists    
$HADOOP_HOME/bin/hdfs dfs -put -f "$INPUT_DATA" "$INPUT_HDFS" || { echo "Error while moving data to hdfs"; exit 1; }
$HADOOP_HOME/bin/hdfs dfs -rm -r "$OUTPUT_HDFS" || echo "Target output $OUTPUT_HDFS clear"

# RUN GIRAPH
$HADOOP_HOME/bin/hadoop jar "$PROJECT_HOME/giraph-cc/target/giraph-mimuw-examples-1.2.0-hadoop-2.8.2-jar-with-dependencies.jar" \
    org.apache.giraph.GiraphRunner org.apache.giraph.examples.mimuw.CCSinglePivot \
    -mc org.apache.giraph.examples.mimuw.CCSinglePivotMaster \
    -vif org.apache.giraph.io.formats.IntIntNullTextInputFormat \
    -vip "$INPUT_HDFS" \
    -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
    -op "$OUTPUT_HDFS" \
    -w 1 \
    -ca giraph.SplitMasterWorker=false \
    || { echo "Giraph error"; exit 1; } \
    |& tee $PROJECT_HOME/run.log

echo "OK"
echo "Check result: $HADOOP_HOME/bin/hdfs dfs -cat $OUTPUT_HDFS/part*"
