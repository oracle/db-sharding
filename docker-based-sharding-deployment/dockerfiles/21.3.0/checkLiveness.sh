#!/bin/bash
#
# LICENSE UPL 1.0
# Since: November, 2020
# Author: paramdeep.saini@oracle.com
# Description: Build script for building RAC container image
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#

export PYTHON="/bin/python"

$PYTHON  $SCRIPT_DIR/scripts/$MAINPY --checkliveness='true'
retcode=$?

 if [ ${retcode} -eq 0 ]; then
    exit 0
 else
    exit 1
 fi
