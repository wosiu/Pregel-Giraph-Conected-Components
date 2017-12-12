#!/bin/bash

if [ -z "$HADOOP_HOME" ]
then
    echo "set HADOOP_HOME variable to point your installation, e.g.: export HADOOP_HOME=/opt/hadoop/hadoop-2.8.2"
    exit 1
fi

deamons=`jps | grep "DataNode\|SecondaryNameNode\|NodeManager\|ResourceManager\|NameNode" | wc -l`

if [[ $deamons != 5 ]]
then
	echo "Some hadoop services are missing"
	echo "There should be: DataNode|SecondaryNameNode|NodeManager|ResourceManager|NameNode"
	echo "But currenly there are:"
	jps
	exit 1
fi

$HADOOP_HOME/bin/hdfs dfs -ls / || { echo "Line $LINENO: I can't even ls datanode. Fix your hadoop."; exit 1; }
$HADOOP_HOME/bin/hdfs dfs -mkdir -p /user/wos_michal/ || { echo "Line $LINENO: Error while creating output dir in hdfs"; exit 1; }
# overwrites if exists    
$HADOOP_HOME/bin/hdfs dfs -put -f "download/small.txt" "/user/wos_michal/input" || { echo "Line $LINENO: Error while moving data to hdfs"; exit 1; }

echo "Looks good"
