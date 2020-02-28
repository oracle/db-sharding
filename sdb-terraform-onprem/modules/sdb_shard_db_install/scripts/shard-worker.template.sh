# Copyright 2017, 2019, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl

#! /bin/bash

if [ -d "${oracle_base_path}" ]
then
	echo "${oracle_base_path} directory exists!"
else
	echo "${oracle_base_path} directory not found!"
    mkdir -p ${oracle_base_path}
fi

if [ -d "${db_home_path}" ]
then
	echo "${db_home_path} directory exists!"
else
	echo "${db_home_path} directory not found!"
    mkdir -p ${db_home_path}
fi

source shard.sh
env | grep ORA

cd ${db_home_path}
./runInstaller -silent -ignorePrereqFailure -waitforcompletion -responseFile ${db_home_path}/db.rsp
sleep 120
echo ${sudo_pass} | sudo -S ${ora_inventory_location}/orainstRoot.sh
echo ${sudo_pass} | sudo -S ${db_home_path}/root.sh
${db_home_path}/bin/netca -silent -responseFile ${db_home_path}/assistants/netca/netca.rsp