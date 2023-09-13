#!/bin/sh
# Param is the instance_id: {0,1,2....}
export JAVA_HOME=`/usr/libexec/java_home -v 17`
java -Djava.system.class.loader=tech.edgx.dee.util.DynamicClassLoader -jar target/dee-java-node-dc1-v0.0.1-SNAPSHOT-jar-with-dependencies.jar -instance_id $1

