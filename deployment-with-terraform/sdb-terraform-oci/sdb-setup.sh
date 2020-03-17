# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl

#! /bin/bash

scriptname=$0

function usage {
    echo "Setup Oracle Sharded Database(SDB) on OCI"
    echo "usage: $scriptname plan | deploy | destroy"
    echo "plan   :  Generate the configuration (*.auto.tfvars) files for review before deployment"
    echo "deploy  :  Deploy Sharded database"
    echo "destroy :  Destroy the sharded database setup"
    echo "-h | -help : Displays usage with all options"
}

if [ "$1" = "-h" ] || [ "$1" = "-help" ]
then
  usage
  exit 0
fi

if [ "$#" -ge  2 ] || [ "$#" -lt  1 ]
then
    usage
    exit 1
fi

if [ "$1" =  "plan" ] || [ "$1" =  "deploy" ]
then
    # Create catalog config 
    terraform apply -target null_resource.catalog_config_consolidator -auto-approve
    
    # Create shard director config
    terraform apply -target null_resource.shard_director_config_consolidator -auto-approve

    # Create shard config 
    terraform apply -target null_resource.shard_config_consolidator -auto-approve

    echo "SDB Deployment configuration has been generated and is ready for review in the *.auto.tfvars files. Once reviewed and modified if necessary, run sdb-setup.sh with deploy option as follows: ./sdb-setup.sh deploy"
fi
if [ "$1" =  "deploy" ]
then 
    terraform apply -auto-approve
    terraform apply -auto-approve
    echo "SDB Deployment has completed"
fi
if [ "$1" =  "destroy" ]
then 
    terraform destroy -auto-approve
    echo "SDB Teardown has been completed"
fi