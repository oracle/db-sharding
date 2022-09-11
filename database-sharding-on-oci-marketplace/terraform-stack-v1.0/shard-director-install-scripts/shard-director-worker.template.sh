# Copyright 2020 Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl

#! /bin/bash

if [ -d "${oracle_base_path}" ]
then
	echo "${oracle_base_path} directory exists!"
else
	echo "${oracle_base_path} directory not found!"
    mkdir -p ${oracle_base_path}
fi

if [ -d "${gsm_home_path}" ]
then
	echo "${gsm_home_path} directory exists!"
else
	echo "${gsm_home_path} directory not found!"
    mkdir -p ${gsm_home_path}
fi

cd ${oracle_base_path}

rm -rf ${gsm_install_folder_name}

source ${oracle_base_path}/shard-director.sh

chmod 700 ${oracle_base_path}/${gsm_zip_name}.zip

unzip -o ${oracle_base_path}/${gsm_zip_name}.zip

cd ${oracle_base_path}/${gsm_install_folder_name}

./runInstaller -silent -waitforcompletion -ignoresysprereqs -responseFile ${oracle_base_path}/gsm_install.rsp -showProgress -lenientInstallMode

echo ${sudo_pass} | sudo -S ${ora_inventory_location}/orainstRoot.sh

echo ${sudo_pass} | sudo -S sudo ${gsm_home_path}/root.sh
