# Deploy Sharded Database with User Defined Sharding using Podman Containers

This page covers the steps to manually deploy a sample Sharded Database with User Defined Sharding using Podman Containers. 

- [Setup Details](#setup-details)
- [Prerequisites](#prerequisites)
- [Deploying Catalog Container](#deploying-catalog-container)
  - [Create Directory](#create-directory)
  - [Create Container](#create-container)
- [Deploying Shard Containers](#deploying-shard-containers)
  - [Create Directories](#create-directories)
  - [Shard1 Container](#shard1-container)
  - [Shard2 Container](#shard2-container)
- [Deploying GSM Container](#deploying-gsm-container)
  - [Create Directory for Master GSM Container](#create-directory-for-master-gsm-container)
  - [Create Master GSM Container](#create-master-gsm-container)
- [Deploying Standby GSM Container](#deploying-standby-gsm-container)  
  - [Create Directory for Standby GSM Container](#create-directory-for-standby-gsm-container)
  - [Create Standby GSM Container](#create-standby-gsm-container)
- [Scale-out an existing Sharded Database](#scale-out-an-existing-sharded-database)
  - [Complete the prerequisite steps before creating Podman Container for new shard](#complete-the-prerequisite-steps-before-creating-podman-container-for-new-shard) 
  - [Create Podman Container for new shard](#create-podman-container-for-new-shard)
  - [Add the new shard Database to the existing Sharded Database](#add-the-new-shard-database-to-the-existing-sharded-database)
  - [Deploy the new shard](#deploy-the-new-shard)
  - [Move chunks](#move-chunks)
- [Scale-in an existing Sharded Database](#scale-in-an-existing-sharded-database)
  - [Confirm the shard to be deleted is present in the list of shards in the Sharded Database](#confirm-the-shard-to-be-deleted-is-present-in-the-list-of-shards-in-the-sharded-database)
  - [Move the chunks out of the shard database which you want to delete](#move-the-chunks-out-of-the-shard-database-which-you-want-to-delete)
  - [Delete the shard database from the Sharded Database](#delete-the-shard-database-from-the-sharded-database)
  - [Confirm the shard has been successfully deleted from the Sharded database](#confirm-the-shard-has-been-successfully-deleted-from-the-sharded-database) 
  - [Remove the Podman Container](#remove-the-podman-container)  
- [Copyright](#copyright)


## Setup Details

This setup involves deploying podman containers for:

* Catalog Database
* Two Shard Databases
* Primary GSM 
* Standby GSM

**NOTE:** You can use Oracle 21c or Oracle 23c RDBMS and GSM Podman Images for this sample deployment. 

**NOTE:** In the current Sample Sharded Database Deployment, we have used Oralce 23c RDBMS and GSM Podman Images.

## Prerequisites

Before using this page to create a sample sharded database, please complete the prerequisite steps mentioned in [Create Containers using manual steps using Podman](./README.md)

Before creating the GSM container, you need to build the catalog and shard containers. Execute the following steps to create containers:

## Deploying Catalog Container

The shard catalog is a special-purpose Oracle Database that is a persistent store for SDB configuration data and plays a key role in the automated deployment and centralized management of a sharded database. It also hosts the gold schema of the application and the master copies of common reference data (duplicated tables)

### Create Directory

You need to create mountpoint on the podman host to save datafiles for Oracle Sharding Catalog DB and expose as a volume to catalog container. This volume can be local on a podman host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this sample Sharded Database, we used /oradata/dbfiles/CATALOG directory and exposed as volume to catalog container.

```
mkdir -p /oradata/dbfiles/CATALOG
chown -R 54321:54321 /oradata/dbfiles/CATALOG
```

**Notes:**:

* Change the ownership for data volume `/oradata/dbfiles/CATALOG` exposed to catalog container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
* If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instance Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

### Create Container

Before performing catalog container, review the following notes carefully:

**Notes:**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/CATALOG based on your enviornment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID enviornment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name catalog oracle/database:23.4.0-ee` to `--name catalog oracle/database:23.4.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, /oradata/dbfiles/CATALOG must contain the DB backup and it must not be in zipped format. E.g. /oradata/dbfiles/CATALOG/SEEDCDB where SEEDCDB is the cold backup and contains datafiles and PDB.

```
podman run -d --hostname oshard-catalog-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.102 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=CATCDB \
 -e ORACLE_PDB=CAT1PDB \
 -e OP_TYPE=catalog \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -e SHARD_SETUP="true" \
 -e ENABLE_ARCHIVELOG=true \
 -v /oradata/dbfiles/CATALOG:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name catalog oracle/database-ext-sharding:23.4.0-ee

    Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside the container
      PWD_KEY:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to catalog.
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts that you want to run after setting up the catalog.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name which must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after catalog setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed clonging using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

To check the catalog container/services creation logs, please tail podman logs. It will take 20 minutes to create the catalog container service.

```
podman logs -f catalog
```

**IMPORTANT:** The resulting images will be an image with the Oracle binaries installed. On first startup of the container a new database will be created, the following lines highlight when the Shard database is ready to be used:
     
```
    ==============================================
         GSM Catalog Setup Completed
    ==============================================
```  

## Deploying Shard Containers

A database shard is a horizontal partition of data in a database or search engine. Each individual partition is referred to as a shard or database shard. You need to create mountpoint on podman host to save datafiles for Oracle Sharding DB and expose as a volume to shard container. This volume can be local on a podman host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/ORCL1CDB directory and exposed as volume to shard container.

### Create Directories

```
mkdir -p /oradata/dbfiles/ORCL1CDB
mkdir -p /oradata/dbfiles/ORCL2CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL1CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL2CDB
```

**Notes:**:

* Change the ownership for data volume `/oradata/dbfiles/ORCL1CDB` and `/oradata/dbfiles/ORCL2CDB` exposed to shard container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
* If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instace Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

### Shard1 Container

Before creating shard1 container, review the following notes carefully:

**Notes:**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/ORCL1CDB based on your environment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID environment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard1 oracle/database:23.4.0-ee` to `--name shard1 oracle/database:23.4.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, `/oradata/dbfiles/ORCL1CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL1CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.

```
podman run -d --hostname oshard1-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.103 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL1CDB \
 -e ORACLE_PDB=ORCL1PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -e SHARD_SETUP="true" \
 -e ENABLE_ARCHIVELOG=true \
 -v /oradata/dbfiles/ORCL1CDB:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name shard1 oracle/database-ext-sharding:23.4.0-ee

   Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside container
      PWD_KEY:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to primaryshard or standbyshard
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts which you want to run after setting up shard setup.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name that must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after shard db setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed cloning using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

To check the shard1 container/services creation logs, please tail podman logs. It will take 20 minutes to create the shard1 container service.

```
podman logs -f shard1
```

### Shard2 Container

Before creating shard1 container, review the following notes carefully:

**Notes:**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/ORCL2CDB based on your environment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID environment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard2 oracle/database:23.4.0-ee` to `--name shard2 oracle/database:23.4.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, `/oradata/dbfiles/ORCL2CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL2CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.

```
podman run -d --hostname oshard2-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.104 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL2CDB \
 -e ORACLE_PDB=ORCL2PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -e SHARD_SETUP="true" \
 -e ENABLE_ARCHIVELOG=true \
 -v /oradata/dbfiles/ORCL2CDB:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name shard2 oracle/database-ext-sharding:23.4.0-ee

     Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside the container
      PWD_KEY:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to primaryshard or standbyshard
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts that you want to run after setting up the shard setup.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name which must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after shard db setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed cloning using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

**Note**: You can add more shards based on your requirement.

To check the shard2 container/services creation logs, please tail podman logs. It will take 20 minutes to create the shard2 container service

```
podman logs -f shard2
```

**IMPORTANT:** The resulting images will be an image with the Oracle binaries installed. On first startup of the container a new database will be created, the following lines highlight when the Shard database is ready to be used:

```
    ==============================================
         GSM Shard Setup Completed
    ==============================================
``` 

## Deploying GSM Container

The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases. You need to create mountpoint on podman host to save gsm setup related file for Oracle Global Service Manager and expose as a volume to GSM container. This volume can be local on a podman host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/GSMDATA directory and exposed as volume to GSM container.

### Create Directory for Master GSM Container

```
mkdir -p /oradata/dbfiles/GSMDATA
chown -R 54321:54321 /oradata/dbfiles/GSMDATA
```

### Create Master GSM Container

```
podman run -d --hostname oshard-gsm1 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.100 \
 -e DOMAIN=example.com \
 -e SHARD_DIRECTOR_PARAMS="director_name=sharddirector1;director_region=region1;director_port=1522" \
 -e CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2;sharding_type=USER;shard_space=shardspace1,shardspace2"     \
 -e SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_space=shardspace1;deploy_as=primary;shard_region=region1" \
 -e SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_space=shardspace2;deploy_as=primary;shard_region=region2" \
 -e SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=primary" \
 -e SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=primary" \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -v /oradata/dbfiles/GSMDATA:/opt/oracle/gsmdata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 -e OP_TYPE=gsm \
 -e MASTER_GSM="TRUE" \
 --privileged=false \
 --name gsm1 oracle/database-gsm:23.4.0

   Mandatory Parameters:
      SHARD_DIRECTOR_PARAMS:     Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=director_name,     value=shard director name
                                 key=director_region,   value=shard director region
                                 key=director_port,     value=shard director port

      CATALOG_PARAMS:            Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=catalog_host,       value=catalog hostname
                                 key=catalog_db,         value=catalog cdb name
                                 key=catalog_pdb,        value=catalog pdb name
                                 key=catalog_port,       value=catalog db port name
                                 key=catalog_name,       value=catalog name in GSM
                                 key=catalog_region,     value=specify comma separated region name for catalog db deployment
                                 key=sharding_type,      value=specify the type of sharding (For example: USER)
                                 key=shard_space,        value=comma separated names for shard spaces to be used in the deployment

      SHARD[1-9]_PARAMS:         Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=shard_host,         value=shard hostname
                                 key=shard_db,           value=shard cdb name
                                 key=shard_pdb,          value=shard pdb name
                                 key=shard_port,         value=shard db port
                                 key=shard_space,        value=shard space name
                                 key=deploy_as,          value=primary or standby
                                 key=shard_region,       value=region name

        **Notes**:
           SHARD[1-9]_PARAMS is in regex form, you can specify env parameter based on your environment such SHARD1_PARAMS, SHARD2_PARAMS.
           Each SHARD[1-9]_PARAMS must have above key value pair.

      SERVICE[1-9]_PARAMS:      Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=service_name,       value=service name
                                 key=service_role,       value=service role e.g. primary or physical_standby
        **Notes**:
           SERVICE[1-9]_PARAMS is in regex form, you can specify env parameter based on your environment such SERVICE1_PARAMS, SERVICE2_PARAMS.
           Each SERVICE[1-9]_PARAMS must have above key value pair.

      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside container
      PWD_KEY:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For GSM it has to be set to gsm.
      DOMAIN:                   Domain of the container.
      MASTER_GSM:               Set value to "TRUE" if you want the GSM to be a master GSM. Otherwise, do not set it.

    Optional Parameters:
      SAMPLE_SCHEMA:            Specify a value to "DEPLOY" if you want to deploy sample app schema in catalog DB during GSM setup.
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts that you want to run after setting up GSM.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name which must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after GSM setup.
      BASE_DIR:                 Specify BASE_DIR if you want to change the base location of the scripts to setup GSM. Note that CUSTOM_SHARD_SCRIPT_DIR/CUSTOM_SHARD_SCRIPT_FILE will run after GSM setup but BASE_DIR specify the location of the scripts to setup the GSM. Default is set to $INSTALL_DIR/startup/scripts.
      SCRIPT_NAME:              Specify the script name which will be executed from BASE_DIR. Default set to main.py.
      EXECUTOR:                 Specify the script executor such as /bin/python or /bin/bash. Default set to /bin/python.
```

**Note:** Change environment variables such as DOMAIN, CATALOG_PARAMS, PRIMARY_SHARD_PARAMS, COMMON_OS_PWD_FILE and PWD_KEY according to your environment.

To check the gsm1 container/services creation logs, please tail podman logs. It will take 2 minutes to create the gsm container service.

```
podman logs -f gsm1
```

## Deploying Standby GSM Container

You need standby GSM container to serve the connection when master GSM fails.

### Create Directory for Standby GSM Container

```
mkdir -p /oradata/dbfiles/GSM2DATA
chown -R 54321:54321 /oradata/dbfiles/GSM2DATA
```

### Create Standby GSM Container

```
podman run -d --hostname oshard-gsm2 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.101 \
 -e DOMAIN=example.com \
 -e SHARD_DIRECTOR_PARAMS="director_name=sharddirector2;director_region=region2;director_port=1522" \
 -e CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2;sharding_type=USER;shard_space=shardspace1,shardspace2" \
 -e SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_space=shardspace1;"  \
 -e SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_space=shardspace2;"  \
 -e SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=standby" \
 -e SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=standby" \
 -e CATALOG_SETUP="True" \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -v /oradata/dbfiles/GSM2DATA:/opt/oracle/gsmdata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 -e OP_TYPE=gsm \
 --privileged=false \
 --name gsm2 oracle/database-gsm:23.4.0

**Note:** Change environment variables such as DOMAIN, CATALOG_PARAMS, COMMON_OS_PWD_FILE and PWD_KEY according to your environment.

   Mandatory Parameters:
      CATALOG_SETUP:             Accept True. if set then , it will only restrict till catalog connection and setup.
      CATALOG_PARAMS:            Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=catalog_host,       value=catalog hostname
                                 key=catalog_db,         value=catalog cdb name
                                 key=catalog_pdb,        value=catalog pdb name
                                 key=catalog_port,       value=catalog db port name
                                 key=catalog_name,       value=catalog name in GSM
                                 key=catalog_region,     value=specify comma separated region name for catalog db deployment
```

To check the gsm2 container/services creation logs, please tail podman logs. It will take 2 minutes to create the gsm container service.

```
podman logs -f gsm2
```

**IMPORTANT:** The resulting images will be an image with the Oracle GSM binaries installed. On first startup of the container a new GSM setup will be created, the following lines highlight when the GSM setup is ready to be used:

```
    ==============================================
         GSM Setup Completed
    ==============================================
```

**NOTE:** Once the Sharded Database Deployment is complete using Used Defined Sharding, you can load the data into this Sharded Database.

## Scale-out an existing Sharded Database

If you want to Scale-Out an existing Sharded Database already deployed using the Podman Containers, then you will to complete the steps in below order:

- Complete the prerequisite steps before creating the Podman Container for the new shard to be added to the Sharded Database
- Create the Podman Container for the new shard
- Add the new shard Database to the existing Sharded Database
- Deploy the new shard
- Move chunks

The below example covers the steps to add a new shard (shard3) to an existing Sharded Database which was deployed earlier in this page with two shards (shard1 and shard2).

### Complete the prerequisite steps before creating Podman Container for new shard

Create the required directories for the new shard (shard3 in this case) container just like they were created for the earlier shards (shard1 and shard2):

```
mkdir -p /oradata/dbfiles/ORCL3CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL3CDB
```

**Notes:**:

* Change the ownership for data volume `/oradata/dbfiles/ORCL3CDB` and `/oradata/dbfiles/ORCL3CDB` exposed to shard container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
* If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instace Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

### Create Podman Container for new shard

Before creating new shard (shard3 in this case) container, review the following notes carefully:

**Notes:**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/ORCL3CDB based on your environment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID environment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard3 oracle/database:23.4.0-ee` to `--name shard3 oracle/database:23.4.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, `/oradata/dbfiles/ORCL3CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL3CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.

```
podman run -d --hostname oshard3-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.105 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL3CDB \
 -e ORACLE_PDB=ORCL3PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=pwdfile.enc \
 -e PWD_KEY=key.pem \
 -e SHARD_SETUP="true" \ 
 -e ENABLE_ARCHIVELOG=true \
 -v /oradata/dbfiles/ORCL3CDB:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name shard3 oracle/database-ext-sharding:23.4.0-ee

   Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside container
      PWD_KEY:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to primaryshard or standbyshard
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts which you want to run after setting up shard setup.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name that must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after shard db setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed cloning using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

To check the shard3 container/services creation logs, please tail podman logs. It will take 20 minutes to create the shard1 container service.

```
podman logs -f shard3
```

**IMPORTANT:** Like the earlier shards (shard1 and shard2), wait for the following lines highlight when the Shard3 database is ready to be used:

```
    ==============================================
         GSM Shard Setup Completed
    ==============================================
```

### Add the new shard Database to the existing Sharded Database

Use the below command to add the new shard3:
```
podman exec -it gsm1 python /opt/oracle/scripts/sharding/scripts/main.py --addshard="shard_host=oshard3-0;shard_db=ORCL3CDB;shard_pdb=ORCL3PDB;shard_port=1521;shard_space=shardspace3;shard_region=region3"
```

Use the below command to check the status of the newly added shard:
``` 
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config shard
```

### Deploy the new shard

Deploy the newly added shard (shard3):

```
podman exec -it gsm1 python /opt/oracle/scripts/sharding/scripts/main.py --deployshard=true
```

Use the below command to check the status of the newly added shard and the chunks distribution:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config shard

podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config chunks
```

### Move chunks

In case you want to move some chunks to the newly added Shard from an existing Shard, you can use the below command:

```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl MOVE CHUNK -CHUNK $CHUNK_ID -SOURCE $SOURCE_SHARD -TARGET $TARGET_SHARD
```

Example: If you want to move the chunk with chunk id `3` from source shard `ORCL1CDB_ORCL1PDB` to target shard `ORCL3CDB_ORCL3PDB`, then you can use the below command:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl MOVE CHUNK -CHUNK 3 -SOURCE ORCL1CDB_ORCL1PDB -TARGET ORCL3CDB_ORCL3PDB
```

Use the below command to check the status of the chunks distribution:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config chunks
```

## Scale-in an existing Sharded Database

If you want to Scale-in an existing Sharded Database by removing a particular shard database out of the existing shard databases, then you will to complete the steps in below order:

- Confirm the shard to be deleted is present in the list of shards in the Sharded Database
- Move the chunks out of the shard database which you want to delete
- Delete the shard database from the Sharded Database
- Confirm the shard has been successfully deleted from the Sharded database


### Confirm the shard to be deleted is present in the list of shards in the Sharded Database

Use the below commands to check the status of the shard which you want to delete and status of chunks present in this shard:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config shard

podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config chunks
```


### Move the chunks out of the shard database which you want to delete

In the current example, if you want to delete the shard3 database from the Sharded Database, then you need to use the below command to move the chunks out of shard3 database:

```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl MOVE CHUNK -CHUNK $CHUNK_ID -SOURCE $SOURCE_SHARD -TARGET $TARGET_SHARD
```

Example: If you want to move the chunk with chunk id `3` from source shard `ORCL3CDB_ORCL3PDB` to target shard `ORCL1CDB_ORCL1PDB`, then you can use the below command:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl MOVE CHUNK -CHUNK 3 -SOURCE ORCL3CDB_ORCL3PDB -TARGET ORCL1CDB_ORCL1PDB
```

**NOTE:** To move more than 1 chunk, you can specify comma separated chunk ids.

After moving the chunks out, use the below command to confirm there is no chunk present in the shard database which you want to delete:
```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config chunks
```

**NOTE:** You will need to wait for some time for all the chunks to move out of the shard database which you want to delete. 

### Delete the shard database from the Sharded Database

Once you have confirmed that no chunk is present in the shard to be deleted in earlier step, you can use the below command to delete that shard(shard3 in this case):
```
podman exec -it gsm1 python /opt/oracle/scripts/sharding/scripts/main.py  --deleteshard="shard_host=oshard3-0;shard_db=ORCL3CDB;shard_pdb=ORCL3PDB;shard_port=1521;shard_space=shardspace3;shard_region=region3"
```

**NOTE:** In this case, `oshard3-0`, `ORCL3CDB` and `ORCL3PDB` are the names of host, CDB and PDB for the shard3 respectively.


### Confirm the shard has been successfully deleted from the Sharded database

Once the shard is deleted from the Sharded Database, use the below commands to check the status of the shards and chunk distribution in the sharded database:

```
podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config shard

podman exec -it gsm1 $(podman exec -it gsm1 env | grep ORACLE_HOME | cut -d= -f2 | tr -d '\r')/bin/gdsctl config chunks
```

### Remove the Podman Container

Once the shard is deleted from the Sharded Database, you can remove the Podman Container which was deployed earlier for the deleted shard database. 

If the deleted shard was `shard3`, to remove its Podman Container, please use the below steps:

- Stop and remove the Docker Container for shard3:
```
podman stop shard3
podman rm shard3
```

- Remove the directory containing the files for this deleted Podman Container:
```
rm -rf /oradata/dbfiles/ORCL3CDB
```

## Copyright

Copyright (c) 2022, 2023 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/