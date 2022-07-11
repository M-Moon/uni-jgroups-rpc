#!/bin/sh

CLASSPATH = "JGroups/jgroups-3.6.20.Final.jar"

javac -cp $CLASSPATH:. client/*.java
javac -cp $CLASSPATH:. server/*.java

if [ $? -eq 0 ]
then
    echo "compile worked!"
fi
