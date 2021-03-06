#!/bin/bash
#
export PROCESS_NAME=con_ws-1.0-jar-with-dependencies.jar
#
echo Locating $PROCESS_NAME in active process table.
#
PID=`ps -ef | grep $PROCESS_NAME  | grep -v grep | awk '{print $2}'`
#
if [ -z "$PID" ]
then
    echo The $PROCESS_NAME  process is already stopped.
else
    echo Terminating the $PROCESS_NAME by process id value $PID.
#   kill -TERM $PID
fi 
exit 0
