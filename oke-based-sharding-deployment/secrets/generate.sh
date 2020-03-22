#!/bin/bash
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# MAINTAINER <paramdeep.saini@oracle.com>

echo "ENV emcrypted Key"
cat key.txt  | base64
echo "Password Encrypted Key"
cat password.txt | base64  
