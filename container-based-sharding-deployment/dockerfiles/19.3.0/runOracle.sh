#!/bin/bash

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################

#This is the main file which calls other file to setup the sharding.
if [ -z ${BASE_DIR} ]; then
    BASE_DIR=$INSTALL_DIR/sharding/scripts
fi

if [ -z ${MAIN_SCRIPT} ]; then
    SCRIPT_NAME="main.py"
fi

if [ -z ${EXECUTOR} ]; then
    EXECUTOR="python"
fi

cd $BASE_DIR
$EXECUTOR $SCRIPT_NAME

# Tail on alert log and wait (otherwise container will exit)

if [ -z ${DEV_MODE} ]; then
 echo "The following output is now a tail of the alert.log:"
 tail -f $ORACLE_BASE/diag/gsm/*/*/trace/alert*.log &
else
 echo "The following output is now a tail of the /etc/passwd for dev mode"
 tail -f /etc/passwd &
fi
 
childPID=$!
wait $childPID
