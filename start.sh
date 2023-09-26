#!/bin/sh
# MacOS
# Param: instance_id: {0,1,2....} which sets port number, intended to support testing multiple nodes in dev environment
export JAVA_HOME=`/usr/libexec/java_home -v 17`
java -Djava.system.class.loader=tech.edgx.rexe.util.DynamicClassLoader \
     -jar target/rexe-java-node-v0.0.1-SNAPSHOT-jar-with-dependencies.jar \
     -instance_id $1

