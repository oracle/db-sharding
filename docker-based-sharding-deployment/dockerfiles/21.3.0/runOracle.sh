#!/bin/bash
# LICENSE UPL 1.0
# Since: November, 2020
# Author: paramdeep.saini@oracle.com
# Description: Build script for building RAC container image
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#

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
