#!/bin/bash
$JAVA_HOME/bin/java \
 -XX:+UseParallelGC \
 -Xmx32g -Xms32g -Xmn31g \
 --add-opens java.base/java.io=ALL-UNNAMED \
 -jar target/bigdecimal-storage-benchmark-0.2.0-SNAPSHOT.jar java10.txt
