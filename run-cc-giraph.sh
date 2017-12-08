#!/bin/bash

# stop on any error
set -e

pushd `dirname $0` > /dev/null
PROJECT_HOME=`pwd -P`
popd > /dev/null

# in my case HADOOP_HOME=/opt/hadoop/hadoop-2.8.2
if [ -z "$HADOOP_HOME" ]
then
    echo "set HADOOP_HOME variable to point your hadoop installation, e.g.: export HADOOP_HOME=/opt/hadoop/hadoop-2.8.2"
    exit 1
fi

#if [ -z "$GIRAPH_HOME" ]
#then
#    echo "set GIRAPH_HOME variable to point your giraph installation, e.g.: export GIRAPH_HOME=/opt/hadoop/giraph"
#    exit 1
#fi

if [[ $USER == "m" ]]
then
    # just for my env    
    export INPUT_DATA="$PROJECT_HOME/download/dblp-2011.txt"
fi

if [ -z "$INPUT_DATA" ]
then
    echo "set INPUT_DATA variable to point graph input file, e.g.: export INPUT_DATA=~/download/dblp-2011.txt"
    exit 1
fi

INPUT_HDFS="/user/wos_michal/input"
OUTPUT_HDFS="/user/wos_michal/output"

pushd $PROJECT_HOME/giraph-examples
    mvn -Phadoop_2 -Dhadoop.version=2.8.2 -DskipTests package || { echo "Error while building"; exit 1; }
popd

pushd $HADOOP_HOME
    #INIT DATA    
    bin/hdfs dfs -mkdir -p /user/wos_michal/
    # overwrites if exists    
    bin/hdfs dfs -put -f "$INPUT_DATA" "$INPUT_HDFS"
    bin/hdfs dfs -rm -r "$OUTPUT_HDFS" || echo "Output in $OUTPUT_HDFS"

    #bin/hadoop jar "$PROJECT_HOME/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.8.2-jar-with-dependencies.jar" \
    #    org.apache.giraph.GiraphRunner -h
    #exit 0    

    # RUN GIRAPH
    bin/hadoop jar "$PROJECT_HOME/giraph-examples/target/giraph-examples-1.3.0-SNAPSHOT-for-hadoop-2.8.2-jar-with-dependencies.jar" \
        org.apache.giraph.GiraphRunner org.apache.giraph.examples.mimuw.CCSinglePivot \
        -mc org.apache.giraph.examples.mimuw.CCSinglePivotMaster \
        -vif org.apache.giraph.io.formats.IntIntNullTextInputFormat \
        -vip "$INPUT_HDFS" \
        -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
        -op "$OUTPUT_HDFS" \
        -w 1 \
        -ca giraph.SplitMasterWorker=false

    echo "OK"
    echo "Check result: $HADOOP_HOME/bin/hdfs dfs -ls $OUTPUT_HDFS"
popd

