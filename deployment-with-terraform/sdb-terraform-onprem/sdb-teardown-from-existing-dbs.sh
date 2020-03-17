#! /bin/bash

scriptname=$0

function usage {
    echo "Script to teardown Oracle Sharded Database (SDB) on existing oracle databases with the following optional arguments"
    echo "usage: $scriptname [with-shard-standby] [with-catalog-standby] [with-demo] [deploy-shard-standby]"
    echo "  with-shard-standby     Teardown SDB with shard standby. Pre-Requisite : DB instance should already be running on the standby machine."
    echo "  with-catalog-standby   Teardown SDB with catalog standby"
    echo "  with-demo              Teardown SDB Demo"
    echo "  deploy-shard-standby   Teardown deployed shard standby for an already configured DG setup. Pre-Requisite : DB instance should already be running on the standby machine along with dataguard setup, configured and FSFO observers started"
    echo "  -h | -help             Displays usage with all options"
}

if [ "$1" = "-h" ] || [ "$1" = "-help" ]
then
  usage
  exit 0
fi

if [ "$#" -gt  4 ]
then
    usage
    exit 1
fi

if [ "$#" -le  4 ]
then
    if [ "$1" = "with-demo" ] || [ "$2" = "with-demo" ] || [ "$3" = "with-demo" ] || [ "$4" = "with-demo" ]
    then
        # Destroy demo
        terraform destroy -target null_resource.sdb_demo_monitor -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform destroy -target null_resource.sdb_demo_setup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        
        # Destroy demo schema
        terraform destroy -target null_resource.sdb_schema_setup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # Destroy deployment
    terraform destroy -target null_resource.sdb_add_service -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_deploy_invoker -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ] || [ "$4" = "with-shard-standby" ] || [ "$1" = "deploy-shard-standby" ] || [ "$2" = "deploy-shard-standby" ] || [ "$3" = "deploy-shard-standby" ] || [ "$4" = "deploy-shard-standby" ]
    then
        terraform destroy -target null_resource.sdb_add_standby_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform destroy -target null_resource.sdb_add_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ]  || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ] || [ "$4" = "with-shard-standby" ] || [ "$1" = "deploy-shard-standby" ] || [ "$2" = "deploy-shard-standby" ] || [ "$3" = "deploy-shard-standby" ] || [ "$4" = "deploy-shard-standby" ]
    then
        terraform destroy -target null_resource.sdb_add_standby_shard_group -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform destroy -target null_resource.sdb_add_shard_group -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Destroy config on shard directors for catalog failover   
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ] || [ "$4" = "with-catalog-standby" ] 
    then
        terraform destroy -target null_resource.sdb_enable_switchover_relocation_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # Destroy configuration of primary shards and standby shards
    terraform destroy -target null_resource.sdb_shard_validation -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ] || [ "$4" = "with-shard-standby" ]
    then
        terraform destroy -target null_resource.sdb_add_static_dg_listener -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform destroy -target null_resource.sdb_setup_data_guard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform destroy -target null_resource.sdb_enable_sys_dg -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform destroy -target null_resource.sdb_shard_db_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ] || [ "$4" = "with-shard-standby" ] || [ "$1" = "deploy-shard-standby" ] || [ "$2" = "deploy-shard-standby" ] || [ "$3" = "deploy-shard-standby" ] || [ "$4" = "deploy-shard-standby" ]
    then
        terraform destroy -target null_resource.sdb_standby_shard_env_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve 
    fi
    terraform destroy -target null_resource.sdb_shard_env_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Destroy shard director configuration
    terraform destroy -target null_resource.sdb_shard_director_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Destroy catalog db configuration
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ] || [ "$4" = "with-catalog-standby" ]
    then
        terraform destroy -target null_resource.sdb_add_static_dg_listener_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform destroy -target null_resource.sdb_setup_data_guard_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform destroy -target null_resource.sdb_enable_sys_dg_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve 
        terraform destroy -target null_resource.sdb_shard_catalog_configure_with_standby -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve      
    fi
    
    if [ "$1" != "with-catalog-standby" ] && [ "$2" != "with-catalog-standby" ] && [ "$3" != "with-catalog-standby" ] && [ "$4" != "with-catalog-standby" ]
    then
        terraform destroy -target null_resource.sdb_shard_catalog_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    
    # Destroy catalog db
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ] || [ "$4" = "with-catalog-standby" ]
    then
        terraform destroy -target null_resource.sdb_shard_catalog_standby_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform destroy -target null_resource.sdb_shard_catalog_db_create -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_catalog_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_catalog_db_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Destroy the database s/w for shards
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ] || [ "$4" = "with-shard-standby" ]
    then
        terraform destroy -target null_resource.sdb_standby_shard_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform destroy -target null_resource.sdb_shard_db_create -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_db_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Destroy the GSM s/w for shard directors
    terraform destroy -target null_resource.sdb_shard_director_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Catalog cleanup
    terraform destroy -target null_resource.sdb_shard_catalog_cleanup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ] || [ "$4" = "with-catalog-standby" ]
    then
        terraform destroy -target null_resource.sdb_shard_catalog_standby_cleanup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve        
    fi

    # Terraform Meta GC
    terraform destroy -target null_resource.sdb_shard_director_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_catalog_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_catalog_standby_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform destroy -target null_resource.sdb_shard_standby_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    echo "SDB teardown completed"
fi