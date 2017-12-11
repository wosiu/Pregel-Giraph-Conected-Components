#!/bin/bash

exec > >(tee -i debug.log)
exec 2>&1

pushd `dirname $0` > /dev/null
PROJECT_HOME=`pwd -P`
popd > /dev/null

echo "=============MAVEN==========="
which mvn
ls -l `which mvn`
mvn --version
echo "=============JAVA============"
which java
ls -l `which java`
java -version
echo "=============BUILD==========="

pushd $PROJECT_HOME/giraph-cc
    mvn -e -X -Phadoop_2 -Dhadoop.version=2.8.2 -DskipTests clean package || { echo "Error while building"; exit 1; }
popd

echo "BUDOWANIE JEST OK :)"
