#!/bin/bash

# stop on any error
set -e

pushd `dirname $0` > /dev/null
PROJECT_HOME=`pwd -P`
popd > /dev/null

# ensure can ssh
ssh localhost -C 'echo "Can ssh"' || {
                echo "Cannot ssh localhost without password, run:"
                echo "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys"
                echo "if you know what you're doing"
                exit 1
        }


# in my case HADOOP_HOME=/opt/hadoop/hadoop-2.8.2
if [ -z "$HADOOP_HOME" ]
then
    echo "set HADOOP_HOME variable to point your installation, e.g.: export HADOOP_HOME=/opt/hadoop/hadoop-2.8.2"
    exit 1
fi

pushd $HADOOP_HOME
    # START HADOOP
    source etc/hadoop/hadoop-env.sh
    # bin/hdfs namenode -format
    sbin/start-dfs.sh
    sbin/start-all.sh
    sleep 3
popd

echo "OK"
