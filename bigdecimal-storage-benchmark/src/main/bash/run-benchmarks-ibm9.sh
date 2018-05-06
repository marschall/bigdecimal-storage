#!/bin/bash
# https://github.com/eclipse/openj9/issues/42
# https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.lnx.80.doc/diag/appendixes/cmdline/xgcpolicy.html
$JAVA_HOME/bin/java \
 -Xgcpolicy:optthruput \
 -Xmx32g -Xms32g -Xmn31g \
 --add-opens java.base/java.io=ALL-UNNAMED \
 -jar target/bigdecimal-storage-benchmark-0.2.0-SNAPSHOT.jar ibm9.txt
