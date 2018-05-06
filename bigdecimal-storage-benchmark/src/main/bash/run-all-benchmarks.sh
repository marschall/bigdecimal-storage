#!/bin/bash

export JAVA_HOME=/home/marschall/bin/java/jdk-8
./src/main/bash/run-benchmarks-java8.sh

export JAVA_HOME=/home/marschall/bin/java/graalvm-8
./src/main/bash/run-benchmarks-graal8.sh

export JAVA_HOME=/home/marschall/bin/java/jdk-10
./src/main/bash/run-benchmarks-java10.sh
./src/main/bash/run-benchmarks-graal10.sh

export JAVA_HOME=/home/marschall/bin/java/openj9-9
./src/main/bash/run-benchmarks-ibm9.sh
