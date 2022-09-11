#! /bin/bash

scriptname=$0

function usage {
    echo "Script to setup Oracle Sharded Database (SDB) with the following optional arguments"
    echo "usage: $scriptname [with-shard-standby] [with-catalog-standby] [with-demo]"
    echo "  with-shard-standby     Setup SDB with shard standby"
    echo "  with-catalog-standby   Setup SDB with catalog standby"
    echo "  with-demo              Setup SDB Demo"
    echo "  -h | -help             Displays usage with all options"
}

if [ "$1" = "-h" ] || [ "$1" = "-help" ]
then
  usage
  exit 0
fi

if [ "$#" -gt  3 ]
then
    usage
    exit 1
fi


if [ "$#" -le  3 ]
then
    # Install the GSM s/w for shard directors 
    terraform apply -target null_resource.sdb_shard_director_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # # Install the database s/w for shards
    terraform apply -target null_resource.sdb_shard_db_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_db_create -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ]
    then
        terraform apply -target null_resource.sdb_standby_shard_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # Install catalog db
    terraform apply -target null_resource.sdb_shard_catalog_db_install -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_catalog_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_catalog_db_create -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_catalog_cleanup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ]
    then
        terraform apply -target null_resource.sdb_shard_catalog_standby_db_install_sw -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_shard_catalog_standby_cleanup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi


    # Configure catalog db
    if [ "$1" != "with-catalog-standby" ] && [ "$2" != "with-catalog-standby" ] && [ "$3" != "with-catalog-standby" ]
    then
        terraform apply -target null_resource.sdb_shard_catalog_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ]
    then
        terraform apply -target null_resource.sdb_shard_catalog_configure_with_standby -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_enable_sys_dg_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_setup_data_guard_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_add_static_dg_listener_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # Configure shard director
    terraform apply -target null_resource.sdb_add_osuser_credential -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    # Configure shard directors for catalog failover
    if [ "$1" = "with-catalog-standby" ] || [ "$2" = "with-catalog-standby" ] || [ "$3" = "with-catalog-standby" ]
    then
        terraform apply -target null_resource.sdb_enable_switchover_relocation_catalog -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # # Configure primary shards and standby shards
    terraform apply -target null_resource.sdb_shard_db_configure -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ]
    then
        terraform apply -target null_resource.sdb_enable_sys_dg -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_setup_data_guard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_add_static_dg_listener -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi
    terraform apply -target null_resource.sdb_shard_validation -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_schagent_register_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ]
    then
        terraform apply -target null_resource.sdb_schagent_register_standby_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # # Deploy
    terraform apply -target null_resource.sdb_add_shard_group -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ]
    then
        terraform apply -target null_resource.sdb_add_standby_shard_group -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi    
    terraform apply -target null_resource.sdb_create_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_create_shard_exec -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    if [ "$1" = "with-shard-standby" ] || [ "$2" = "with-shard-standby" ] || [ "$3" = "with-shard-standby" ]
    then
        terraform apply -target null_resource.sdb_create_standby_shard -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_create_standby_shard_exec -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve     
    fi
    terraform apply -target null_resource.sdb_deploy_invoker -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_add_service -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    if [ "$1" = "with-demo" ] || [ "$2" = "with-demo" ] || [ "$3" = "with-demo" ]
    then
        echo "SDB setup with demo"

        # Setup Demo Schema
        terraform apply -target null_resource.sdb_schema_setup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

        # Setup Demo
        terraform apply -target null_resource.sdb_demo_setup -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
        terraform apply -target null_resource.sdb_demo_monitor -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    fi

    # Terraform Meta GC
    terraform apply -target null_resource.sdb_shard_director_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_catalog_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_catalog_standby_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve
    terraform apply -target null_resource.sdb_shard_standby_gc -var-file=terraform.tfvars -var-file=secrets.tfvars -auto-approve

    echo "SDB deployment completed"
fi