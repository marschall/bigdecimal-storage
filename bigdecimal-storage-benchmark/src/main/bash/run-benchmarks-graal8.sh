#!/bin/bash
$JAVA_HOME/bin/java \
 -XX:+UseParallelGC \
 -Xmx32g -Xms32g -Xmn31g \
 -jar target/bigdecimal-storage-benchmark-0.2.0-SNAPSHOT.jar graal8.txt
