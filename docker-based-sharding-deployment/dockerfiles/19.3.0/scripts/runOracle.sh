#!/bin/bash
# LICENSE UPL 1.0
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#
# Since: January, 2020
# Author: sanjay.singh@oracle.com, paramdeep.saini@oracle.com

########### SIGINT handler ############
function _int() {
   echo "Stopping container."
   echo "SIGINT received, shutting down database!"
   sqlplus / as sysdba <<EOF
   shutdown immediate;
   exit;
EOF
   lsnrctl stop
}

########### SIGTERM handler ############
function _term() {
   echo "Stopping container."
   echo "SIGTERM received, shutting down database!"
   sqlplus / as sysdba <<EOF
   shutdown immediate;
   exit;
EOF
   lsnrctl stop
}

########### SIGKILL handler ############
function _kill() {
   echo "SIGKILL received, shutting down database!"
   sqlplus / as sysdba <<EOF
   shutdown abort;
   exit;
EOF
   lsnrctl stop
}


# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! #
############# MAIN ################
# !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! #
###################################


if [ ${SHARD_SETUP,,} == "true" ]; then
 sh $ORACLE_BASE/scripts/sharding/runOraShardSetup.sh
fi

if [ ! -z ${CLONE_DB} ]; then
 if [ ${CLONE_DB^^} == "TRUE" ]; then
  echo "The following output is now a tail of the alert.log:"
  tail -f $ORACLE_BASE/diag/rdbms/*/*/trace/alert*.log &
 fi
fi

if [ ! -z ${OP_TYPE} ]; then
 if [ ${OP_TYPE,,} == "standbyshard" ]; then
   echo "The following output is now a tail of the alert.log:"
   tail -f $ORACLE_BASE/diag/rdbms/*/*/trace/alert*.log &
 fi
fi

# Set SIGINT handler
trap _int SIGINT

# Set SIGTERM handler
trap _term SIGTERM

# Set SIGKILL handler
trap _kill SIGKILL


childPID=$!
wait $childPID
