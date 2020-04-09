# Oracle Sharding on Docker
Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

Sample Docker build files to facilitate installation, configuration, and environment setup for DevOps users. For more information about Oracle Database please see the [Oracle Sharded Database Management Documentation](http://docs.oracle.com/en/database/).

## How to build and run
This project offers sample Dockerfiles for:
  * Oracle Database 19c Client (19.3) for Linux x86-64

To assist in building the images, you can use the [buildDockerImage.sh](dockerfiles/buildDockerImage.sh) script.See section **Create Oracle Global Service Manager Image** for instructions and usage.

**IMPORTANT:** Oracle Global Service Manager container is useful when you want to configure Global Data Service Framework. The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases. 

For complete Oracle Sharding Database setup, please go through following steps and execute them as per your environment:

### Create Oracle Global Service Manager Image
**IMPORTANT:** You will have to provide the installation binaries of Oracle Global Service Manager Oracle Database 19c  (19.3) for Linux x86-64 and put them into the `dockerfiles/<version>` folder. You  only need to provide the binaries for the edition you are going to install. The binaries can be downloaded from the [Oracle Technology Network](http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). You also have to make sure to have internet connectivity for yum. Note that you must not uncompress the binaries.

The `buildDockerImage.sh` script is just a utility shell script that performs MD5 checks and is an easy way for beginners to get started. Expert users are welcome to directly call `docker build` with their preferred set of parameters.Before you build the image make sure that you have provided the installation binaries and put them into the right folder. Go into the **dockerfiles** folder and run the **buildDockerImage.sh** script as root or with sudo privileges:

```
./buildDockerImage.sh -v (Software Version)
./buildDockerImage.sh -v 19.3.0
```
For detailed usage of command, please execute following command:
```
./buildDockerImage.sh -h
```
### Create Oracle Database Image
You can create Oracle 19.3 Database image by following the steps provided in [oracle/docker-images](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance). 

**Note**: You just need to create the image. You will create the container as per the steps given in this document.

### Create Network Bridge
Before creating container, create the macvlan bridge. If you are using same bridge with same network then you can use same IPs mentioned in **Create Containers** section.

```
# docker network create -d macvlan --subnet=172.16.1.0/24 --gateway=172.16.1.1 -o parent=eth0 shard_pub1_nw
```

If you are planing to create a test env within a single machine, you can use docker bridge but these IPs will not reachable on user network.

```
# docker network create --driver=bridge --subnet=172.16.1.0/24 shard_pub1_nw
```

**Note:** You can change subnet according to your environment.

### Create Containers.
Before creating the GSM container, you must setup Oracle Shard Catalog and oracle Shards. Download and create Oracle database images. Database version must match with GSM version. To download and build Oracle 19.3 Database image, please refer [README.MD](https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md)  of Oracle Single Database available on Oracle GitHub repository.

Once the Oracle database 19.3.0 image is created, you need to build the catalog and shard containers. Execute following steps to create containers:

#### Setup Hostfile
All containers will share a host file for name resolution.  The shared hostfile must be available to all container. Create the shared host file (if it doesn't exist) at `/opt/containers/shard_host_file`:

For example:

```
# mkdir /opt/containers
# touch /opt/containers/shard_host_file
```

Add following host entries in /opt/containers/shard_host_file as Oracle database containers do not have root access to modify the /etc/hosts file. This file must be pre-populated. You can change these entries based on your environment and network setup.

```
127.0.0.1       localhost.localdomain   localhost
172.16.1.15     oshard-gsm1.example.com  oshard-gsm1
172.16.1.20     oshard-catalog-0.example.com  oshard-catalog-0
172.16.1.21     oshard1-0.example.com   oshard1-0
172.16.1.22     oshard2-0.example.com   oshard2-0
172.16.1.23     oshard3-0.example.com   oshard3-0
```

#### Copy User Scripts to setup Env
From the cloned Oracle Sharding repository, you need to copy Sharding/dockerfile/<version/setupOshardEnv.sh to some other directory to expose to DB containers. In our example, we created /tmp/oshard and copied the file under that location.

#### Password Setup
Specify the secret volume for resetting database users password during catalog and shard setup. It can be shared volume among all the containers

```
mkdir /opt/.secrets/
openssl rand -hex 64 -out /opt/.secrets/pwd.key
```

Edit the `/opt/.secrets/common_os_pwdfile` and seed the password for grid/oracle and database. It will be a common password for all the database users. Execute following command:

```
openssl enc -aes-256-cbc -salt -in /opt/.secrets/common_os_pwdfile -out /opt/.secrets/common_os_pwdfile.enc -pass file:/opt/.secrets/pwd.key
rm -f /opt/.secrets/common_os_pwdfile
```

#### Deploying Catalog Container
The shard catalog is a special-purpose Oracle Database that is a persistent store for SDB configuration data and plays a key role in automated deployment and centralized management of a sharded database. It also hosts the gold schema of the application and the master copies of common reference data (duplicated tables)

```
docker run -d --hostname oshard-catalog-0 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=172.16.1.20 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=CATCDB \
 -e ORACLE_PDB=CAT1PDB \
 -e OP_TYPE=catalog \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -v /docker_volumes/oradata:/opt/oracle/oradata \
 -v /tmp/oshard/scripts:/opt/oracle/scripts/setup \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets \
 --privileged=false \
 --name catalog oracle/database:19.3.0-ee
```

**Note**: Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env. Also, change the datafile volume location. In the above example, it is set to /docker_volumes/oradata.

To check the catalog container/services creation logs , please tail docker logs. It will take 20 minutes to create the catalog container service.

```
docker logs catalog
```

#### Deploying Shard Containers
A database shard is a horizontal partition of data in a database or search engine. Each individual partition is referred to as a shard or database shard.

##### Shard1 Container
```
docker run -d --hostname oshard1-0 \
  --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=172.16.1.21 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL1CDB \
 -e ORACLE_PDB=ORCL1PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -v /docker_volumes/oradata:/opt/oracle/oradata \
 -v /tmp/oracle-sharding-si-k8s/scripts:/opt/oracle/scripts/setup \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets \
 --privileged=false \
 --name shard1 oracle/database:19.3.0-ee
```

**Note:** Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env. Also, change the datafile volume location. In the above example, it is set to /docker_volumes/oradata.

To check the shard1 container/services creation logs , please tail docker logs. It will take 20 minutes to create the shard1 container service.

```
docker logs shard1
```

##### Shard2 Container
```
docker run -d --hostname oshard2-0 \
  --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=172.16.1.22 \
 -e DOMAIN=example.com \
 -e ORACLE_SID=ORCL2CDB \
 -e ORACLE_PDB=ORCL2PDB \
 -e OP_TYPE=primaryshard \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -v /docker_volumes/oradata:/opt/oracle/oradata \
 -v /tmp/oracle-sharding-si-k8s/scripts:/opt/oracle/scripts/setup \
 -v /opt/containers/shard_host_file:/etc/hosts \
 --volume /opt/.secrets:/run/secrets \
 --privileged=false \
  --name shard2 oracle/database:19.3.0-ee
```
**Note:** Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env. Also, change the datafile volume location. In the above example, it is set to /docker_volumes/oradata.

**Note**: You can add more shards based on your requirement.

To check the shard2 container/services creation logs , please tail docker logs. It will take 20 minutes to create the shard2 container service
```
docker logs shard2
```

#### Deploying GSM Container
The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases.

```
docker run -d --hostname oshard-gsm1 \
 --dns-search=example.com \
 --network=shard_pub1_nw \
 --ip=172.16.1.15 \
 -e DOMAIN=example.com \
 -e CATALOG_PARAMS="oshard-catalog-0:CATCDB:CAT1PDB" \
 -e PRIMARY_SHARD_PARAMS="oshard1-0:ORCL1CDB:ORCL1PDB;oshard2-0:ORCL2CDB:ORCL2PDB"  \
 -e COMMON_OS_PWD_FILE=common_os_pwdfile.enc \
 -e PWD_KEY=pwd.key \
 -e REGION=region1 \
 --volume /docker_volumes/oradata:/opt/oracle/gsmdata \
 --volume /opt/containers/shard_host_file:/etc/hosts \
 -v /tmp/oracle-sharding-si-k8s/scripts:/opt/oracle/scripts/setup \
 --volume /opt/.secrets:/run/secrets \
 -e OP_TYPE=gsm \
 --privileged=false \
 --name gsm1 oracle/databse-gsm:19.3.0
```

**Note:** Change environment variable such as DOMAIN, CATALOG_PARAMS, PRIMARY_SHARD_PARAMS, COMMON_OS_PWD_FILE and PWD_KEY according to your environment. 

To check the gsm1 container/services creation logs , please tail docker logs. It will take 2 minutes to create the gsm container service.

```
docker logs gsm1
```

You should see following when cman container setup is done:

```
###################################
GSM IS READY TO USE!
###################################
```

