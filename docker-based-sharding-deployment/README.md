# Oracle Sharding on Docker
Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

This project offers sample container files to facilitate installation, configuration, and environment setup for DevOps users. For more information about Oracle Database please see the [Oracle Sharded Database Management Documentation](http://docs.oracle.com/en/database/).

## How to build and run
Please review README of following sections in a given order. After reviewing the README of each section, you can skip the image/container creation if you do not meet the requirement.

* Please review following points before you proceed to next sections:
  * Install Docker engine
    * Please refer to [Oracle Container Runtime for Docker Documentation](https://docs.oracle.com/cd/E52668_01/E87205/html/index.html) to install and configure docker-engine. You can use XFS file system with overlay2 file system.

This project offers sample container files:
  * Oracle Database 21c Global Service Manager (GSM/GDS) (21.3) for Linux x86-64
  * Oracle Database 19c Global Service Manager (GSM/GDS) (19.3) for Linux x86-64

To assist in building the images, you can use the [buildContainerImage.sh](dockerfiles/buildContainerImage.sh) script.See section **Create Oracle Global Service Manager Image** for instructions and usage.

**IMPORTANT:** Oracle Global Service Manager container is useful when you want to configure the Global Data Service Framework. The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases. 

## Using GSM and Sharding Image
To deploy a Oracle Sharding topology, please execute the steps in the following sections below:
1. [Create Oracle Global Service Manager Image](#create-oracle-global-service-manager-image)
2. [Create Oracle Database Image](#create-oracle-database-image)
3. [Create Extended Oracle Database Image with Sharding Feature](#create-extended-oracle-database-image-with-sharding-feature)
4. [Create Network Bridge](#create-network-bridge)
5. [Password Setup](#password-setup)
6. [Deploying Catalog Container](#deploying-catalog-container)
7. [Deploying Shard Containers](#deploying-shard-containers)
8. [Deploying GSM Container](#deploying-gsm-container)
9. [Support](#support)
10. [License](#license) 
12. [Copyright](#copyright) 

### Create Oracle Global Service Manager Image
**IMPORTANT:** You will have to provide the installation binaries of Oracle Global Service Manager Oracle Database 21c  (21.3) for Linux x86-64 and put them into the `dockerfiles/<version>` folder. You only need to provide the binaries for the edition you are going to install. The binaries can be downloaded from the [Oracle Technology Network](http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). You also have to make sure to have internet connectivity for yum. 
**Note:** You must not uncompress the binaries.

The `buildContainerImage.sh` script is just a utility shell script that performs MD5 checks and is an easy way for beginners to get started. Expert users are welcome to directly call `docker build` with their preferred set of parameters. Before you build the image make sure that you have provided the installation binaries and put them into the right folder. Go into the **dockerfiles** folder and run the **buildContainerImage.sh** the script as root or with sudo privileges:

```
./buildContainerImage.sh -v (Software Version)
./buildContainerImage.sh -v 21.3.0
```
For detailed usage of command, please execute following command:
```
./buildContainerImage.sh -h
```
### Create Oracle Database Image
To build Oracle Sharding on docker/container, you need to download and build Oracle 21.3 Database image, please refer [README.MD](https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md) of Oracle Single Database available on Oracle GitHub repository.

**Note**: You just need to create the image as per the instructions given in [README.MD](https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md) but you will create the container as per the steps given in this document under [Create Containers](#create-containers) section.

### Create Extended Oracle Database Image with Sharding Feature
After creating the base image using buildContainerImage.sh in the previous step, use buildExtensions.sh present under the extensions folder to build an extended image that will include the Sharding Feature. Please refer [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/extensions/README.md) of extensions folder of Oracle Single Database available on Oracle GitHub repository.

For example: 
```
./buildExtensions.sh -a -x sharding -b oracle/database:21.3.0-ee  -t oracle/database-ext-sharding:21.3.0-ee 

Where:
"-x sharding"					is to specify to have sharding feature in the extended image
"-b oracle/database:21.3.0-ee" 			is to specify the Base image created in previous step
"oracle/database-ext-sharding:21.3.0-ee"	is to specify the name:tag for the extended image with Sharding Feature
```

### Create Network Bridge
Before creating a container, create the docker network by creating docker network bridge based on your enviornment. If you are using the bridge name with the network subnet mentioned in this README.md then you can use the same IPs mentioned in [Create Containers](#create-containers) section.

#### Macvlan Bridge

```
# docker network create -d macvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=eth0 shard_pub1_nw
```

#### Ipvlan Bridge
```
# docker network create -d ipvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=eth0 shard_pub1_nw
```

If you are planning to create a test env within a single machine, you can use a docker bridge but these IPs will not be reachable on the user network.
#### Bridge

```
# docker network create --driver=bridge --subnet=10.0.20.0/24 shard_pub1_nw
```

**Note:** You can change subnet and choose one of the above mentioned docker network bridge based on your enviornment.

### Create Containers
Before creating the GSM container, you need to build the catalog and shard containers. Execute the following steps to create containers:

#### Setup Hostfile
All containers will share a host file for name resolution.  The shared hostfile must be available to all containers. Create the shared host file (if it doesn't exist) at `/opt/containers/shard_host_file`:

For example:

```
# mkdir /opt/containers
# touch /opt/containers/shard_host_file
```

Add the following host entries in /opt/containers/shard_host_file as Oracle database containers do not have root access to modify the /etc/hosts file. This file must be pre-populated. You can change these entries based on your environment and network setup.

```
127.0.0.1       localhost.localdomain           localhost
10.0.20.100     oshard-gsm1.example.com         oshard-gsm1
10.0.20.101     oshard-gsm2.example.com         oshard-gsm2
10.0.20.102     oshard-catalog-0.example.com    oshard-catalog-0
10.0.20.103     oshard1-0.example.com           oshard1-0
10.0.20.104     oshard2-0.example.com           oshard2-0
10.0.20.105     oshard3-0.example.com           oshard3-0
10.0.20.106     oshard4-0.example.com           oshard4-0
```
#### Password Setup
Specify the secret volume for resetting database users password during catalog and shard setup. It can be a shared volume among all the containers

```
mkdir /opt/.secrets/
openssl rand -hex 64 -out /opt/.secrets/pwd.key
```

Edit the `/opt/.secrets/common_os_pwdfile` and seed the password for grid/oracle and database. It will be a common password for all the database users. Execute following command:  
```
vi /opt/.secrets/common_os_pwdfile
```
**Note**: Enter your secure password in the above file and save the file.

After seeding password and saving the `/opt/.secrets/common_os_pwdfile` file, execute following command:  
```
openssl enc -aes-256-cbc -salt -in /opt/.secrets/common_os_pwdfile -out /opt/.secrets/common_os_pwdfile.enc -pass file:/opt/.secrets/pwd.key
rm -f /opt/.secrets/common_os_pwdfile
chown 54321:54321 /opt/.secrets/common_os_pwdfile.enc
chown 54321:54321 /opt/.secrets/pwd.key
chmod 400 /opt/.secrets/common_os_pwdfile.enc
chmod 400 /opt/.secrets/pwd.key
```

This password is being used for initial sharding topology setup. Once the sharding topology setup is completed, user must change the sharding topology passwords based on his enviornment.

#### Deploying Catalog Container
The shard catalog is a special-purpose Oracle Database that is a persistent store for SDB configuration data and plays a key role in the automated deployment and centralized management of a sharded database. It also hosts the gold schema of the application and the master copies of common reference data (duplicated tables)

##### Create Directory
You need to create mountpoint on the docker host to save datafiles for Oracle Sharding Catalog DB and expose as a volume to catalog container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/CATALOG directory and exposed as volume to catalog container.

```
mkdir -p /oradata/dbfiles/CATALOG
chown -R 54321:54321 /oradata/dbfiles/CATALOG
```

**Notes**: 
 * Change the ownership for data volume `/oradata/dbfiles/CATALOG` exposed to catalog container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
 * If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instance Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

##### Create Container

Before performing catalog container, review the following notes carefully:
  
**Notes**
 * Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
 * Change /oradata/dbfiles/CATALOG based on your enviornment.
 * By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID enviornment variable.
 * If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name catalog oracle/database:21.3.0-ee` to `--name catalog oracle/database:21.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
   * In this case, /oradata/dbfiles/CATALOG must contain the DB backup and it must not be in zipped format. E.g. /oradata/dbfiles/CATALOG/SEEDCDB where SEEDCDB is the cold backup and contains datafiles and PDB. 
```
docker run -d --hostname oshard-catalog-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.102 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=CATCDB \
 -e ORACLE_PDB=CAT1PDB \
 -e OP_TYPE=catalog \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -e SHARD_SETUP="true" \
 -v /oradata/dbfiles/CATALOG:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name catalog oracle/database:21.3.0-ee
 
    Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside the ontainer
      PWD.key:                  Specify password key file to decrypt the encrypted password file and read the password
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

To check the catalog container/services creation logs, please tail docker logs. It will take 20 minutes to create the catalog container service.

```
docker logs -f catalog
```

**IMPORTANT:** The resulting images will be an image with the Oracle binaries installed. On first startup of the container a new database will be created, the following lines highlight when the Shard database is ready to be used:

    ################################################
	Oracle GSM Catalog Setup Completed Successfully!
	################################################
	
#### Deploying Shard Containers
A database shard is a horizontal partition of data in a database or search engine. Each individual partition is referred to as a shard or database shard. You need to create mountpoint on docker host to save datafiles for Oracle Sharding DB and expose as a volume to shard container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/ORCL1CDB directory and exposed as volume to shard container.

##### Create Directories
```
mkdir -p /oradata/dbfiles/ORCL1CDB
mkdir -p /oradata/dbfiles/ORCL2CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL2CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL1CDB
```

**Notes**: 
 * Change the ownership for data volume `/oradata/dbfiles/ORCL1CDB` and `/oradata/dbfiles/ORCL2CDB` exposed to shard container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
 * If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instace Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

##### Shard1 Container

Before creating shard1 container, review the following notes carefully:  

**Notes**
 * Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
 * Change /oradata/dbfiles/ORCL1CDB based on your enviornment.
 * By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID enviornment variable.
 * If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard1 oracle/database:21.3.0-ee` to `--name shard1 oracle/database:21.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
   * In this case, `/oradata/dbfiles/ORCL1CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL1CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.

```
docker run -d --hostname oshard1-0 \
  --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.103 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL1CDB \
 -e ORACLE_PDB=ORCL1PDB \
 -e OP_TYPE=primaryshard \
 -e SHARD_SETUP="true" \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -v /oradata/dbfiles/ORCL1CDB:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name shard1 oracle/database:21.3.0-ee
 
   Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside container
      PWD.key:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to primaryshard or standbyshard
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts which you want to run after setting up shard setup.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name that must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after shard db setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed clonging using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

To check the shard1 container/services creation logs, please tail docker logs. It will take 20 minutes to create the shard1 container service.

```
docker logs -f shard1
```

##### Shard2 Container
Before creating shard1 container, review the following notes carefully:  

**Notes**
 * Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
 * Change /oradata/dbfiles/ORCL2CDB based on your enviornment.
 * By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID enviornment variable.
 * If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard2 oracle/database:21.3.0-ee` to `--name shard2 oracle/database:21.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
   * In this case, `/oradata/dbfiles/ORCL2CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL2CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.

```
docker run -d --hostname oshard2-0 \
  --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=10.0.20.104 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL2CDB \
 -e ORACLE_PDB=ORCL2PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -e SHARD_SETUP="true" \
 -v /oradata/dbfiles/ORCL2CDB:/opt/oracle/oradata \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets:ro \
 --privileged=false \
 --name shard2 oracle/database:21.3.0-ee
  
     Mandatory Parameters:
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside the container
      PWD.key:                  Specify password key file to decrypt the encrypted password file and read the password
      OP_TYPE:                  Specify the operation type. For Shards it has to be set to primaryshard or standbyshard
      DOMAIN:                   Specify the domain name
      ORACLE_SID:               CDB name
      ORACLE_PDB:               PDB name

    Optional Parameters:
      CUSTOM_SHARD_SCRIPT_DIR:  Specify the location of custom scripts that you want to run after setting up the shard setup.
      CUSTOM_SHARD_SCRIPT_FILE: Specify the file name which must be available on CUSTOM_SHARD_SCRIPT_DIR location to be executed after shard db setup.
      CLONE_DB: Specify value "true" if you want to avoid db creation and clone it from cold backup of existing Oracle DB. This DB must not have shard setup. Shard script will look for the backup at /opt/oracle/oradata.
      OLD_ORACLE_SID: Specify the OLD_ORACLE_SID if you are performing db seed clonging using existing cold backup of Oracle DB.
      OLD_ORACLE_PDB: Specify the OLD_ORACLE_PDB if you are performing db seed cloning using existing cold backup of Oracle DB.
```

**Note**: You can add more shards based on your requirement.

To check the shard2 container/services creation logs, please tail docker logs. It will take 20 minutes to create the shard2 container service
```
docker logs -f shard2
```

**IMPORTANT:** The resulting images will be an image with the Oracle binaries installed. On first startup of the container a new database will be created, the following lines highlight when the Shard database is ready to be used:

    ##############################################
	Oracle GSM Shard Setup Completed Successfully!
	###############################################
	
#### Deploying GSM Container
The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases. You need to create mountpoint on docker host to save gsm setup related file for Oracle Global Service Manager and expose as a volume to GSM container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/GSMDATA directory and exposed as volume to GSM container.

##### Create Directory
```
mkdir -p /oradata/dbfiles/GSMDATA
chown -R 54321:54321 /oradata/dbfiles/GSMDATA
```

##### Create GSM Master Container
```
  docker run -d --hostname oshard-gsm1 \
   --dns-search=example.com \
   --network=shard_pub1_nw \
   --ip=10.0.20.100 \
   -e DOMAIN=example.com \
   -e SHARD_DIRECTOR_PARAMS="director_name=sharddirector1;director_region=region1;director_port=1522" \
   -e SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=primary;group_region=region1" \
   -e CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2" \
   -e SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"  \
   -e SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"  \
   -e SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=primary" \
   -e SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=primary" \
   -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
   -e PWD_KEY=pwd.key \
   -v /oradata/dbfiles/GSMDATA:/opt/oracle/gsmdata \
   -v /opt/containers/shard_host_file:/etc/hosts \
   --volume /opt/.secrets:/run/secrets:ro \
   -e OP_TYPE=gsm \
   -e MASTER_GSM="TRUE" \
   --privileged=false \
   --name gsm1 oracle/database-gsm:21.3.0
   
   Mandatory Parameters:
      SHARD_DIRECTOR_PARAMS:     Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=director_name,     value=shard director name
                                 key=director_region,   value=shard director region
                                 key=director_port,     value=shard director port
                                 
      SHARD[1-9]_GROUP_PARAMS:   Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=group_name,        value=shard group name
                                 key=deploy_as,         value=deploy shard group as primary or active_standby
                                 key=group_region,      value=shard group region name
         **Notes**: 
           SHARD[1-9]_GROUP_PARAMS is in regex form, you can specify env parameter based on your enviornment such SHARD1_GROUP_PARAMS, SHARD2_GROUP_PARAMS.
           Each SHARD[1-9]_GROUP_PARAMS must have above key value pair.
         
      CATALOG_PARAMS:            Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=catalog_host,       value=catalog hostname
                                 key=catalog_db,         value=catalog cdb name
                                 key=catalog_pdb,        value=catalog pdb name
                                 key=catalog_port,       value=catalog db port name
                                 key=catalog_name,       value=catalog name in GSM
                                 key=catalog_region,     value=specify comma separated region name for catalog db deployment
                                 
      SHARD[1-9]_PARAMS:         Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=shard_host,         value=shard hostname 
                                 key=shard_db,           value=shard cdb name
                                 key=shard_pdb,          value=shard pdb name
                                 key=shard_port,         value=shard db port
                                 key=shard_group         value=shard group name
        **Notes**: 
           SHARD[1-9]_PARAMS is in regex form, you can specify env parameter based on your enviornment such SHARD1_PARAMS, SHARD2_PARAMS.
           Each SHARD[1-9]_PARAMS must have above key value pair.
                                  
      SERVICE[1-9]_PARAMS:      Accept key value pair separated by semicolon e.g. <key>=<value>;<key>=<value> for following <key>=<value> pairs:
                                 key=service_name,       value=service name
                                 key=service_role,       value=service role e.g. primary or physical_standby
        **Notes**: 
           SERVICE[1-9]_PARAMS is in regex form, you can specify env parameter based on your enviornment such SERVICE1_PARAMS, SERVICE2_PARAMS.
           Each SERVICE[1-9]_PARAMS must have above key value pair. 
           
      COMMON_OS_PWD_FILE:       Specify the encrypted password file to be read inside container
      PWD.key:                  Specify password key file to decrypt the encrypted password file and read the password
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

To check the gsm1 container/services creation logs, please tail docker logs. It will take 2 minutes to create the gsm container service.

```
docker logs -f gsm1
```

#### Create GSM Standby Container
You need GSM standby container to serve the connection when master GSM fails.

##### Create Directory
```
mkdir -p /oradata/dbfiles/GSM2DATA
chown -R 54321:54321 /oradata/dbfiles/GSM2DATA
```

##### Create Container
```
  docker run -d --hostname oshard-gsm2 \
   --dns-search=example.com \
   --network=shard_pub1_nw \
   --ip=10.0.20.101 \
   -e DOMAIN=example.com \
   -e SHARD_DIRECTOR_PARAMS="director_name=sharddirector2;director_region=region1;director_port=1522" \
   -e SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=active_standby;group_region=region1" \
   -e CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2" \
   -e CATALOG_SETUP="True" \
   -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
   -e PWD_KEY=pwd.key \
   -v /oradata/dbfiles/GSM2DATA:/opt/oracle/gsmdata \
   -v /opt/containers/shard_host_file:/etc/hosts \
   --volume /opt/.secrets:/run/secrets:ro \
   -e OP_TYPE=gsm \
   --privileged=false \
   --name gsm2 oracle/database-gsm:21.3.0
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

To check the gsm2 container/services creation logs, please tail docker logs. It will take 2 minutes to create the gsm container service.

```
docker logs -f gsm2
```

**IMPORTANT:** The resulting images will be an image with the Oracle GSM binaries installed. On first startup of the container a new GSM setup will be created, the following lines highlight when the GSM setup is ready to be used:

```
    ##############################################
	Oracle GSM Setup Completed Successfully!
	###############################################
```

## Support 

Oracle GSM and Sharding Database is supported for Oracle Linux 7.

## License 

To download and run Oracle GSM and Sharding Database, regardless whether inside or outside a Docker container, you must download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker images are, unless otherwise noted, released under UPL 1.0 license.

## Copyright 

Copyright (c) 2014-2021 Oracle and/or its affiliates.
