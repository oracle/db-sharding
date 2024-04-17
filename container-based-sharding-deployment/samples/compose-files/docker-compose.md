# Create Containers using docker compose

Below steps provide an example to use `docker-compose` to create the docker network and to create the containers for a Sharded Database Deployment on a single Oracle Linux 7 host using Docker Containers.

**IMPORTANT:** This example uses 21c RDBMS and 21c GSM Docker Images.

- [Step 1: Install Docker Compose](#install-docker-compose)
- [Step 2: Complete the prerequisite steps](#complete-the-prerequisite-steps)
- [Step 3: Create Docker Compose file](#create-docker-compose-file)
- [Step 4: Create services using "docker compose" command](#create-services-using-docker-compose-command)
- [Step 5: Check the logs](#check-the-logs)
- [Step 6: Remove the deployment](#remove-the-deployment)
- [Copyright](#copyright)


## Install Docker Compose
```bash
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
ls -lrt $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.23.1/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
ls -lrt $DOCKER_CONFIG/cli-plugins
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose
```

## Complete the prerequisite steps
```bash
    # Export the variables:
    export DOCKERVOLLOC='/scratch/oradata'
    export NETWORK_INTERFACE='ens3'
    export NETWORK_SUBNET="10.0.20.0/20"
    export SIDB_IMAGE='oracle/database-ext-sharding:21.3.0-ee'
    export GSM_IMAGE='oracle/gsm:21.3.0'
    export LOCAL_NETWORK=10.0.20
    export CATALOG_INIT_SGA_SIZE=2048M
    export CATALOG_INIT_PGA_SIZE=800M
    export ALLSHARD_INIT_SGA_SIZE=2048M
    export ALLSHARD_INIT_PGA_SIZE=800M
    export healthcheck_interval=30s
    export healthcheck_timeout=3s
    export healthcheck_retries=40
    export CATALOG_OP_TYPE="catalog"
    export ALLSHARD_OP_TYPE="primaryshard"
    export GSM_OP_TYPE="gsm"
    export COMMON_OS_PWD_FILE="pwdfile.enc"
    export PWD_KEY="key.pem"
    export CAT_SHARD_SETUP="true"
    export CATALOG_ARCHIVELOG="true"
    export SHARD_ARCHIVELOG="true"
    export SHARD1_SHARD_SETUP="true"
    export SHARD2_SHARD_SETUP="true"
    export SHARD3_SHARD_SETUP="true"
    export SHARD4_SHARD_SETUP="true"
    export PRIMARY_GSM_SHARD_SETUP="true"
    export STANDBY_GSM_SHARD_SETUP="true"

    export CONTAINER_RESTART_POLICY="always"
    export CONTAINER_PRIVILEGED_FLAG="false"
    export DOMAIN="example.com"
    export DNS_SEARCH="example.com"
    export CAT_CDB="CATCDB"
    export CAT_PDB="CAT1PDB"
    export CAT_HOSTNAME="oshard-catalog-0"
    export CAT_CONTAINER_NAME="catalog"

    export SHARD1_CONTAINER_NAME="shard1"
    export SHARD1_HOSTNAME="oshard1-0"
    export SHARD1_CDB="ORCL1CDB"
    export SHARD1_PDB="ORCL1PDB"

    export SHARD2_CONTAINER_NAME="shard2"
    export SHARD2_HOSTNAME="oshard2-0"
    export SHARD2_CDB="ORCL2CDB"
    export SHARD2_PDB="ORCL2PDB"

    export PRIMARY_GSM_CONTAINER_NAME="gsm1"
    export PRIMARY_GSM_HOSTNAME="oshard-gsm1"
    export STANDBY_GSM_CONTAINER_NAME="gsm2"
    export STANDBY_GSM_HOSTNAME="oshard-gsm2"

    export PRIMARY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector1;director_region=region1;director_port=1522"
    export PRIMARY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=primary;group_region=region1"
    export PRIMARY_CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2"
    export PRIMARY_SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"
    export PRIMARY_SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"
    export PRIMARY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=primary"
    export PRIMARY_SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=primary"

    export STANDBY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector2;director_region=region2;director_port=1522"
    export STANDBY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=standby;group_region=region2"
    export STANDBY_CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2"
    export STANDBY_SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=standby"
    export STANDBY_SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=standby"

  # Create file with Host IPs for containers
  mkdir -p  /opt/containers
  touch /opt/containers/shard_host_file
  sh -c "cat << EOF > /opt/containers/shard_host_file
  127.0.0.1        localhost.localdomain           localhost
    ${LOCAL_NETWORK}.100     oshard-gsm1.example.com         oshard-gsm1
    ${LOCAL_NETWORK}.102     oshard-catalog-0.example.com    oshard-catalog-0
    ${LOCAL_NETWORK}.103     oshard1-0.example.com           oshard1-0
    ${LOCAL_NETWORK}.104     oshard2-0.example.com           oshard2-0
    ${LOCAL_NETWORK}.105     oshard3-0.example.com           oshard3-0
    ${LOCAL_NETWORK}.106     oshard4-0.example.com           oshard4-0
    ${LOCAL_NETWORK}.101     oshard-gsm2.example.com         oshard-gsm2
EOF
"

# Create an encrypted password file. We have used password "Oracle_21c" here and used the location "/opt/.secrets" to create the encrypted password file:

rm -rf /opt/.secrets/
mkdir /opt/.secrets/
openssl genrsa -out /opt/.secrets/key.pem
openssl rsa -in /opt/.secrets/key.pem -out /opt/.secrets/key.pub -pubout
echo Oracle_21c > /opt/.secrets/pwdfile.txt
openssl pkeyutl -in /opt/.secrets/pwdfile.txt -out /opt/.secrets/pwdfile.enc -pubin -inkey /opt/.secrets/key.pub -encrypt

rm -f /opt/.secrets/pwdfile.txt
chown 54321:54321 /opt/.secrets/pwdfile.enc
chown 54321:54321 /opt/.secrets/key.pem
chown 54321:54321 /opt/.secrets/key.pub
chmod 400 /opt/.secrets/pwdfile.enc
chmod 400 /opt/.secrets/key.pem
chmod 400 /opt/.secrets/key.pub


# Create required directories
mkdir -p ${DOCKERVOLLOC}/scripts
chown -R 54321:54321 ${DOCKERVOLLOC}/scripts
chmod 755 ${DOCKERVOLLOC}/scripts

mkdir -p ${DOCKERVOLLOC}/dbfiles/CATALOG
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/CATALOG

mkdir -p ${DOCKERVOLLOC}/dbfiles/ORCL1CDB
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/ORCL1CDB
mkdir -p ${DOCKERVOLLOC}/dbfiles/ORCL2CDB
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/ORCL2CDB

mkdir -p ${DOCKERVOLLOC}/dbfiles/GSMDATA
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/GSMDATA

mkdir -p ${DOCKERVOLLOC}/dbfiles/GSM2DATA
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/GSM2DATA


chmod 755 ${DOCKERVOLLOC}/dbfiles/CATALOG
chmod 755 ${DOCKERVOLLOC}/dbfiles/ORCL1CDB
chmod 755 ${DOCKERVOLLOC}/dbfiles/ORCL2CDB
chmod 755 ${DOCKERVOLLOC}/dbfiles/GSMDATA
chmod 755 ${DOCKERVOLLOC}/dbfiles/GSM2DATA
```

## Create Docker Compose file 

In this step, copy a Docker Compose file named [docker-compose.yaml](./docker-compose.yml) in your working directory.


## Create services using "docker compose" command
Once you have completed the prerequisties, run below commands to create the services:
```bash
# Switch to location with the `docker-compose.yaml` file and run:
 
docker compose up -d

``` 

## Check the logs

```bash
# You can monitor the logs for all the containers using below command:
 
docker compose logs -f
```

Wait for all setup to be ready:
```
# docker compose up -d
[+] Running 6/6
 ✔ Container pcatalog  Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard1    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard2    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard3    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container gsm1      Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container gsm2      Started       
```

## Remove the deployment

You can also use the `docker-compose` command to remove the deployment. To remove the deployment:

- First export all the variables from the Prerequisites Sesion.
- Use the below command to remove the deployment:

```bash
docker-compose down
```

## Copyright

Copyright (c) 2022, 2023 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/