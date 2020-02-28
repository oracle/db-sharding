- [Terraform modules for deployment of Oracle Sharded Database](#terraform-modules-for-deployment-of-oracle-sharded-database)
  * [About](#about)
  * [Pre-requisites](#pre-requisites)
  * [Environment variables](#environment-variables)
  * [Quickstart](#quickstart)
    + [Initialize Terraform](#initialize-terraform)
    + [View Terraform's SDB Deployment blueprint](#view-terraform-s-sdb-deployment-blueprint)
    + [Setup & Deploy Oracle Sharded database in new_install mode](#setup---deploy-oracle-sharded-database-in-new-install-mode)
      - [Command Usage](#command-usage)
      - [SDB Setup command example](#sdb-setup-command-example)
    + [For tearing down the Sharded Database deployment setup previously in the new_install mode](#for-tearing-down-the-sharded-database-deployment-setup-previously-in-the-new-install-mode)
      - [Command Usage](#command-usage-1)
      - [SDB Teardown command example](#sdb-teardown-command-example)
    + [Setup & Deploy Oracle Sharded database in from_existing_dbs mode](#setup---deploy-oracle-sharded-database-in-from-existing-dbs-mode)
      - [Command Usage](#command-usage-2)
      - [SDB Setup command options](#sdb-setup-command-options)
    + [For tearing down the Sharded Database deployment setup previously in the from_existing_dbs mode](#for-tearing-down-the-sharded-database-deployment-setup-previously-in-the-from-existing-dbs-mode)
      - [Command Usage](#command-usage-3)
      - [SDB Teardown command options](#sdb-teardown-command-options)
  * [Current limitations of SDB Terraform OnPrem](#current-limitations-of-sdb-terraform)


[terraform]: https://releases.hashicorp.com/terraform/0.11.13/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-Demo-zip]: https://support.oracle.com/epmos/faces/DocumentDisplay?id=2226341.1
[DG-Troubleshoot]: https://docs.oracle.com/en/database/oracle/oracle-database/19/dgbkr/troubleshooting-oracle-data-guard-broker.html#GUID-5B9D54C8-C446-4678-A770-2851C41C9265
[SDB-Deploy-Doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-deployment.html#GUID-4E77F1B8-F665-40C4-B4AC-B321C7302AA9
[DG-Doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/dgbkr/oracle-data-guard-broker-installation-requirements.html#GUID-21393DF3-FD7E-44AA-A90C-6533E03CBDDA
[SDB-Issues]: https://github.com/oracle/db-sharding/issues
[SDB-Repo]: https://github.com/oracle/db-sharding
[SDB-Demo]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-deployment.html#GUID-A3433C97-90F8-4CBF-ADA8-2AE2145612DB
[SDB-Deploy-Method]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-deployment.html#GUID-F9C2D239-0F1C-4805-A7FA-1D3D5EBC61FA

# Terraform modules for deployment of Oracle Sharded Database


## About

Provides Oracle Sharded Database (SDB) Terraform modules that provisions the necessary components and resources required for a quick and easy setup of [Oracle Sharded Database][SDB]. It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

## Pre-requisites

1. Download [Terraform][terraform] (v0.11.13) zip and extract the <em>terraform</em> binary to any directory on the machine 
   (will be referred to as current machine in the rest of the documentation) where you are going to clone or download the sdb-terraform-onprem repository and want to invoke the SDB terraform scripts from.  
2. Target hosts are created and Oracle Linux 7+ OS installed on them.
3. Required ports are open on the target machines. (1522, 6123 and 6234 on shard directors and 1521 on shards and catalog(s))
4. Copy public key of the current machine to the remote target hosts.

   ssh-copy-id username@targethost
    
   OR

   scp \~/.ssh/id_rsa.pub username@targethost:\~/.ssh/authorized_keys

5. Place the Global Service Manager(GSM) and Database(DB) binary zips in a directory where they can be accessed on the current machine and provide the paths for them in terraform.tfvars after you download SDB terraform source.
 

## Environment variables

1. Set proxy environment variables if you're running behind a proxy in your current machine's ~/.bashrc and save it.

```
 export http_proxy=http://<address_of_your_proxy>.com:80/
 export https_proxy=http://<address_of_your_proxy>:80/
```

2. Add the following exports to your current machine's ~/.bashrc and save it.

```
 export PATH=<replace-with-path-to-terraform-binary>:$PATH
 export TF_LOG=
```

## Quickstart

1. Download SDB Terraform source and setup input files.
    ```
    $ bash
    $ source ~/.bashrc  
    $ git clone https://github.com/oracle/db-sharding.git  (Using HTTPS) 
      OR 
    $ git clone git@github.com:oracle/db-sharding.git      (Using SSH)
    $ cd sdb-terraform-onprem
    $ cp sdb-*.terraform.tfvars.example terraform.tfvars
    $ cp sdb-*.secrets.tfvars.example secrets.tfvars
    $ chmod 700 *.sh
    ```
2. Optionally, if you also want to setup the [SDB Demo application][SDB-Demo] with sample (dummy) data in all the shards and monitor the shards, copy the [SDB demo zip][SDB-Demo-zip] and place the sdb_demo_app.zip file into the directory where you downloaded terraform scripts at the following path : {sdb-terraform-onprem}/modules/sdb_demo_setup/demo-binaries/
    
3. Set variables in terraform.tfvars and secrets.tfvars. See the variables.tf file for description on each of the variables 
   found in the *.tfvars files. Also refer to the *.tfvars example files for valid values of properties for variables.

* <strong> Note </strong> : Don't remove any variables defined in *.tfvars defined irrespective of the mode or option you are running the setup or teardown scripts with.

4.  Follow the steps below to initialize, plan and deploy in the mode that you want Oracle Sharded Database to be setup.

### Initialize Terraform

```
$ terraform init
```

Initialize step downloads any terraform modules needed and initializes the terraform state machine.

### View Terraform's SDB Deployment blueprint

Note : This is an optional step

```
$ terraform plan -var-file=terraform.tfvars -var-file=secrets.tfvars
```

Verify what will be provisioned and configured in terms of SDB components. Make sure you read and understand the impact of variables in *.tfvars files.


### Setup & Deploy Oracle Sharded database in new_install mode

This mode uses the [create shard method][SDB-Deploy-Method] of deploying Oracle Sharded Database. 

#### Command Usage
```
./sdb-setup-new-install.sh -h 
./sdb-setup-new-install.sh -help
```

#### SDB Setup command example 
```
$ nohup ./sdb-setup-new-install.sh with-catalog-standby with-demo with-shard-standby >> nohup-setup-new-install.log 2>&1 &
```

Verify that everything was successfully setup from the nohup output log. If there are any deployment errors for a particular sdb terraform resource, please attempt to fix the specific resource in error manually if possible and then re-run the setup or teardown script. If the deployment is left in a inconsistent state and you can't recover even after attempting to fix the input configuration, lookup the corresponding teardown section. Please destroy \*.tfstate.\* files in your sdb-terraform workspace post such issues before attempting to re-run setup after running the corresponding teardown script.

If the errors still persist, lookup the error codes on Oracle database or Oracle Sharded database release documentation. If the issue still persists and you think this is a bug with sharded database terraform modules, then file an [issue][SDB-Issues] or send a pull request on [SDB repository][SDB-Repo]. If you think it is a configuration issue that you are facing or need further clarification, then refer the SDB Terraform documentation or contact the Oracle support. 


### For tearing down the Sharded Database deployment setup previously in the new_install mode 

#### Command Usage
```
./sdb-teardown-new-install.sh -h 
./sdb-teardown-new-install.sh -help
```

#### SDB Teardown command example
```
nohup ./sdb-teardown-new-install.sh with-catalog-standby with-demo with-shard-standby >> nohup-teardown-new-install.log 2>&1 &
```

### Setup & Deploy Oracle Sharded database in from_existing_dbs mode

This mode uses the [add shard method][SDB-Deploy-Method] of deploying Oracle Sharded Database.

#### Command Usage
```
./sdb-setup-from-existing-dbs.sh -h 
./sdb-setup-from-existing-dbs.sh -help
```
#### SDB Setup command options

When running setup in the from_existing_dbs mode, there are two options for setting up standbys both for shards and catalog.

1.  If you run the command leaving out the with-shard-standby OR with-catalog-standby option, then you will have to create the primary database instance and standby db instance and make sure the listeners are up and running on both primary and standby dbs. After SDB deployment has succesfully completed, if you encounter any data guard related issues then refer to the [data guard troubleshooting documentation][DG-Troubleshoot]

Make sure to pass the deploy-shard-standby option as shown in the example command below, if you choose to leave out the with-shard-standby option, so that standby shard(s) that you have setup is deployed. <strong> Note </strong> : When you pass deploy-shard-standby option, DO NOT pass the with-shard-standby option as these options are mutually exclusive.

```
$ nohup ./sdb-setup-from-existing-dbs.sh deploy-shard-standby with-catalog-standby with-demo >> nohup-setup-from-existing-dbs.log 2>&1 &
```

2. If you run the below command and pass it the with-shard-standby OR the with-catalog-standby option, then you only have to  create the primary database instance and install the database software onto the standby machine. Make sure the listener is running on the primary database instance. DO NOT start the listener on the standby. <strong> Note </strong> : When you pass with-shard-standby option DO NOT pass the deploy-shard-standby option as these options are mutually exclusive.

```
$ nohup ./sdb-setup-from-existing-dbs.sh  with-shard-standby with-catalog-standby with-demo >> nohup-setup-from-existing-dbs.log 2>&1 &
```

For a complete list of pre-requisites before running the above setup command for the two options specifed above, please refer to the SDB release documentation (SDB release doc for the DB version you are currently using) in the [sharded database deployment chapter][SDB-Deploy-Doc]


If you want to manually setup dataguard then please refer to [Dataguard documention][DG-Doc].

Verify that everything was successfully setup from the nohup output log. If there are any deployment errors for a particular sdb terraform resource, please attempt to fix the specific resource in error manually if possible and then re-run the setup or teardown script. If the deployment is left in a inconsistent state and you can't recover even after attempting to fix the input configuration, lookup the corresponding teardown section. Please destroy \*.tfstate.\* files in your sdb-terraform workspace post such issues before attempting to re-run setup after running the corresponding teardown script.

If the errors still persist, lookup the error codes on Oracle database or Oracle Sharded database release documentation. If the issue still persists and you think this is a bug with sharded database terraform modules, then file an [issue][SDB-Issues] or send a pull request on [SDB repository][SDB-Repo]. If you think it is a configuration issue that you are facing or need further clarification, then refer the SDB Terraform documentation or contact the Oracle support. 


### For tearing down the Sharded Database deployment setup previously in the from_existing_dbs mode

#### Command Usage
```
./sdb-teardown-from-existing-dbs.sh -h 
./sdb-teardown-from-existing-dbs.sh -help
```

#### SDB Teardown command options

When running teardown in the from_existing_dbs mode, there are two options for tearing down standbys both for shards and catalog.

1. If you chose option 1 during setup and passed the deploy-shard-standby option, then make sure to pass the same option for the teardown script as well, as shown below.

```
$ nohup ./sdb-teardown-from-existing-dbs.sh deploy-shard-standby with-catalog-standby with-demo >> nohup-teardown-from-existing-dbs.log 2>&1 &
```

2. If you chose option 2 during setup and passed the with-shard-standby option, then make sure to pass the same option for the teardown script as well, as shown below.

```
nohup ./sdb-teardown-from-existing-dbs.sh with-shard-standby with-catalog-standby with-demo >> nohup-teardown-from-existing-dbs.log 2>&1 &
```

## Current limitations of SDB Terraform On-Premise

1. Doesn't support user-defined and composite sharding.
2. Doesn't support Oracle Golden Gate (OGG) replication.
3. Doesn't support Oracle Container Database (CDB).