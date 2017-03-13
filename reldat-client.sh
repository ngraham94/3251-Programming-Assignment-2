#!/bin/sh

java -classpath client/build/libs/client.jar:lib/build/libs/lib.jar Client $1 $2
