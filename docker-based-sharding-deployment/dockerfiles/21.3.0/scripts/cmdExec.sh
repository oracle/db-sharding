#!/bin/bash

TIMESTAMP=`date "+%Y-%m-%d"`
LOGFILE="/tmp/sharding_cmd_${TIMESTAMP}.log"

echo $(date -u) " : " $@ >> $LOGFILE 

cmd=$@

$cmd
