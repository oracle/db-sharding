#!/bin/bash

# read the options

# Now take action
/bin/python /opt/oracle/scripts/setup/main.py --createdir=/opt/oracle/oradata/ORCL2CDB  --optype=primaryshard

pid=$!
wait $pid
status=$?

 if [ $status -eq 0 ]; then
    exit 0
 else
    exit 1
 fi
