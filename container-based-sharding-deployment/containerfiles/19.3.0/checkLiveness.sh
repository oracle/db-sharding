#!/bin/bash

export PYTHON="/bin/python"

$PYTHON  $SCRIPT_DIR/scripts/$MAINPY --checkliveness='true'
retcode=$?

 if [ ${retcode} -eq 0 ]; then
    exit 0
 else
    exit 1
 fi
