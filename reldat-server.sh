#!/bin/sh

PORT=$1
WINDOW=$2

java -classpath server/build/libs/server.jar:lib/build/libs/lib.jar Server $PORT $WINDOW
