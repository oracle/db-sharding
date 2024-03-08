# Create Containers using docker compose

Below steps provide an example to use "docker-compose" to create the docker network and to create the containers for a Sharded Database Deployment on a single Oracle Linux 7 host using Docker Containers.

**IMPORTANT:** This example uses 21c RDBMS and 21c GSM Docker Images.

- [Step 1: Install Docker Compose](#install-docker-compose)
- [Step 2: Complete the prerequisite steps](#complete-the-prerequisite-steps)
- [Step 3: Create Docker Compose file](#create-docker-compose-file)
- [Step 4: Create services using "docker compose" command](#create-services-using-docker-compose-command)
- [Step 5: Check the logs](#check-the-logs)
- [Step 6: Remove the deployment](#remove-the-deployment)
- [Copyright](#copyright)


## Install Docker Compose
```
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
ls -lrt $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.23.1/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
ls -lrt $DOCKER_CONFIG/cli-plugins
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose
```

## Complete the prerequisite steps
```
# Export the variables:
export DOCKERVOLLOC='/oradata/DOCKER_TEST'
export NETWORK_INTERFACE='ens3'
export SIDB_IMAGE='oracle/database-ext-sharding:21.3.0-ee'
export GSM_IMAGE='oracle/database-gsm:21.3.0'
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
export SHARD1_SHARD_SETUP="true"
export SHARD2_SHARD_SETUP="true"
export SHARD3_SHARD_SETUP="true"
export PRIMARY_GSM_SHARD_SETUP="true"
export STANDBY_GSM_SHARD_SETUP="true"

export CONTAINER_RESTART_POLICY="always"
export CONTAINER_PRIVILEGED_FLAG="false"
export DOMAIN="example.com"
export DNS_SEARCH="example.com"
export CAT_CDB="PCATCDB"
export CAT_PDB="PCAT1PDB"
export CAT_HOSTNAME="pshard-catalog-0"
export CAT_CONTAINER_NAME="pcatalog"

export SHARD1_CONTAINER_NAME="shard1"
export SHARD1_HOSTNAME="pshard1-0"
export SHARD1_CDB="PORCL1CDB"
export SHARD1_PDB="PORCL1PDB"

export SHARD2_CONTAINER_NAME="shard2"
export SHARD2_HOSTNAME="pshard2-0"
export SHARD2_CDB="PORCL2CDB"
export SHARD2_PDB="PORCL2PDB"

export SHARD3_CONTAINER_NAME="shard3"
export SHARD3_HOSTNAME="pshard3-0"
export SHARD3_CDB="PORCL3CDB"
export SHARD3_PDB="PORCL3PDB"


export PRIMARY_GSM_CONTAINER_NAME="gsm1"
export PRIMARY_GSM_HOSTNAME="oshard-gsm1"
export STANDBY_GSM_CONTAINER_NAME="gsm2"
export STANDBY_GSM_HOSTNAME="oshard-gsm2"


export PRIMARY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector1;director_region=primary;director_port=1522"
export PRIMARY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=primary;group_region=primary"
export PRIMARY_CATALOG_PARAMS="catalog_host=pshard-catalog-0;catalog_db=PCATCDB;catalog_pdb=PCAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=primary,standby"
export PRIMARY_SHARD1_PARAMS="shard_host=pshard1-0;shard_db=PORCL1CDB;shard_pdb=PORCL1PDB;shard_port=1521;shard_group=shardgroup1"
export PRIMARY_SHARD2_PARAMS="shard_host=pshard2-0;shard_db=PORCL2CDB;shard_pdb=PORCL2PDB;shard_port=1521;shard_group=shardgroup1"
export PRIMARY_SHARD3_PARAMS="shard_host=pshard3-0;shard_db=PORCL3CDB;shard_pdb=PORCL3PDB;shard_port=1521;shard_group=shardgroup1"
export PRIMARY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=primary"
export PRIMARY_SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=primary"

export STANDBY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector2;director_region=standby;director_port=1522"
export STANDBY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=standby;group_region=standby"
export STANDBY_CATALOG_PARAMS="catalog_host=pshard-catalog-0;catalog_db=PCATCDB;catalog_pdb=PCAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=primary,standby"
export STANDBY_SHARD1_PARAMS="shard_host=pshard1-0;shard_db=PORCL1CDB;shard_pdb=PORCL1PDB;shard_port=1521;shard_group=shardgroup1"
export STANDBY_SHARD2_PARAMS="shard_host=pshard2-0;shard_db=PORCL2CDB;shard_pdb=PORCL2PDB;shard_port=1521;shard_group=shardgroup1"
export STANDBY_SHARD3_PARAMS="shard_host=pshard3-0;shard_db=PORCL3CDB;shard_pdb=PORCL3PDB;shard_port=1521;shard_group=shardgroup1"
export STANDBY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=standby"
export STANDBY_SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=standby"


# Create file with Host IPs for containers
mkdir -p  /opt/containers
touch /opt/containers/shard_host_file
  sh -c "cat << EOF > /opt/containers/shard_host_file
    127.0.0.1        localhost.localdomain           localhost
    ${LOCAL_NETWORK}.153     oshard-gsm1.example.com         oshard-gsm1
    ${LOCAL_NETWORK}.150     pshard-catalog-0.example.com    pshard-catalog-0
    ${LOCAL_NETWORK}.151     pshard1-0.example.com           pshard1-0
    ${LOCAL_NETWORK}.152     pshard2-0.example.com           pshard2-0
    ${LOCAL_NETWORK}.155     pshard3-0.example.com           pshard3-0
    ${LOCAL_NETWORK}.156     pshard4-0.example.com           pshard4-0
    ${LOCAL_NETWORK}.154     oshard-gsm2.example.com         oshard-gsm2

    ## Standby Enteries
    ${LOCAL_NETWORK}.157     sshard-catalog-0.example.com    sshard-catalog-0
    ${LOCAL_NETWORK}.158     sshard1-0.example.com           sshard1-0
    ${LOCAL_NETWORK}.159     sshard2-0.example.com           sshard2-0
    ${LOCAL_NETWORK}.160     sshard3-0.example.com           sshard3-0
    ${LOCAL_NETWORK}.161     sshard4-0.example.com           sshard4-0
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

mkdir -p ${DOCKERVOLLOC}/dbfiles/PCATALOG
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/PCATALOG

mkdir -p ${DOCKERVOLLOC}/dbfiles/PORCL1CDB
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/PORCL1CDB
mkdir -p ${DOCKERVOLLOC}/dbfiles/PORCL2CDB
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/PORCL2CDB
mkdir -p ${DOCKERVOLLOC}/dbfiles/PORCL3CDB
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/PORCL3CDB

mkdir -p ${DOCKERVOLLOC}/dbfiles/GSM1DATA
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/GSM1DATA

mkdir -p ${DOCKERVOLLOC}/dbfiles/GSM2DATA
chown -R 54321:54321 ${DOCKERVOLLOC}/dbfiles/GSM2DATA


chmod 755 ${DOCKERVOLLOC}/dbfiles/PCATALOG
chmod 755 ${DOCKERVOLLOC}/dbfiles/PORCL1CDB
chmod 755 ${DOCKERVOLLOC}/dbfiles/PORCL2CDB
chmod 755 ${DOCKERVOLLOC}/dbfiles/GSM1DATA
chmod 755 ${DOCKERVOLLOC}/dbfiles/GSM2DATA
```

## Create Docker Compose file 

In this step, create a Docker Compose file named "docker-compose.yaml":
```
---
version: "3.8"
networks:
  shard_pub1_nw:
    driver: bridge
    ipam:
      config:
        - subnet: "10.0.20.0/20"
services:
  catalog_db:
    container_name: ${CAT_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${CAT_HOSTNAME}
    image: ${SIDB_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/PCATALOG:/opt/oracle/oradata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${CAT_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      ORACLE_SID: ${CAT_CDB}
      ORACLE_PDB: ${CAT_PDB}
      OP_TYPE: ${CATALOG_OP_TYPE}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      INIT_SGA_SIZE: ${CATALOG_INIT_SGA_SIZE}
      INIT_PGA_SIZE: ${CATALOG_INIT_PGA_SIZE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.150
    healthcheck:
      test: ["CMD-SHELL","if [ `cat /tmp/test.log | grep -c 'GSM Catalog Setup Completed'` -ge 1 ]; then exit 0;else exit 1;fi"]
      interval: ${healthcheck_interval}
      timeout: ${healthcheck_timeout}
      retries: ${healthcheck_retries}
  shard1_db:
    container_name: ${SHARD1_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${SHARD1_HOSTNAME}
    image: ${SIDB_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/PORCL1CDB:/opt/oracle/oradata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${SHARD1_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      ORACLE_SID: ${SHARD1_CDB}
      ORACLE_PDB: ${SHARD1_PDB}
      OP_TYPE: ${ALLSHARD_OP_TYPE}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      INIT_SGA_SIZE: ${ALLSHARD_INIT_SGA_SIZE}
      INIT_PGA_SIZE: ${ALLSHARD_INIT_PGA_SIZE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.151
    healthcheck:
      test: ["CMD-SHELL","if [ `cat /tmp/test.log | grep -c 'GSM Shard Setup Completed'` -ge 1 ]; then exit 0;else exit 1;fi"]
      interval: ${healthcheck_interval}
      timeout: ${healthcheck_timeout}
      retries: ${healthcheck_retries}
  shard2_db:
    container_name: ${SHARD2_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${SHARD2_HOSTNAME}
    image: ${SIDB_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/PORCL2CDB:/opt/oracle/oradata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${SHARD2_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      ORACLE_SID: ${SHARD2_CDB}
      ORACLE_PDB: ${SHARD2_PDB}
      OP_TYPE: ${ALLSHARD_OP_TYPE}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      INIT_SGA_SIZE: ${ALLSHARD_INIT_SGA_SIZE}
      INIT_PGA_SIZE: ${ALLSHARD_INIT_PGA_SIZE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.152
    healthcheck:
      test: ["CMD-SHELL","if [ `cat /tmp/test.log | grep -c 'GSM Shard Setup Completed'` -ge 1 ]; then exit 0;else exit 1;fi"]
      interval: ${healthcheck_interval}
      timeout: ${healthcheck_timeout}
      retries: ${healthcheck_retries}
  shard3_db:
    container_name: ${SHARD3_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${SHARD3_HOSTNAME}
    image: ${SIDB_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/PORCL3CDB:/opt/oracle/oradata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${SHARD3_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      ORACLE_SID: ${SHARD3_CDB}
      ORACLE_PDB: ${SHARD3_PDB}
      OP_TYPE: ${ALLSHARD_OP_TYPE}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      INIT_SGA_SIZE: ${ALLSHARD_INIT_SGA_SIZE}
      INIT_PGA_SIZE: ${ALLSHARD_INIT_PGA_SIZE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.155
    healthcheck:
      test: ["CMD-SHELL","if [ `cat /tmp/test.log | grep -c 'GSM Shard Setup Completed'` -ge 1 ]; then exit 0;else exit 1;fi"]
      interval: ${healthcheck_interval}
      timeout: ${healthcheck_timeout}
      retries: ${healthcheck_retries}
  primary_gsm:
    container_name: ${PRIMARY_GSM_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${PRIMARY_GSM_HOSTNAME}
    image: ${GSM_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/GSM1DATA:/opt/oracle/gsmdata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${PRIMARY_GSM_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      SHARD_DIRECTOR_PARAMS: ${PRIMARY_SHARD_DIRECTOR_PARAMS}
      SHARD1_GROUP_PARAMS: ${PRIMARY_SHARD1_GROUP_PARAMS}
      CATALOG_PARAMS: ${PRIMARY_CATALOG_PARAMS}
      SHARD1_PARAMS: ${PRIMARY_SHARD1_PARAMS}
      SHARD2_PARAMS: ${PRIMARY_SHARD2_PARAMS}
      SHARD3_PARAMS: ${PRIMARY_SHARD3_PARAMS}
      SERVICE1_PARAMS: ${PRIMARY_SERVICE1_PARAMS}
      SERVICE2_PARAMS: ${PRIMARY_SERVICE2_PARAMS}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      MASTER_GSM: 'True'
      OP_TYPE: ${GSM_OP_TYPE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.153
    depends_on:
      catalog_db:
        condition: service_healthy
      shard1_db:
        condition: service_healthy
      shard2_db:
        condition: service_healthy
      shard3_db:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL","if [ `cat /tmp/test.log | grep -c 'GSM Setup Completed'` -ge 1 ]; then exit 0;else exit 1;fi"]
      interval: ${healthcheck_interval}
      timeout: ${healthcheck_timeout}
      retries: ${healthcheck_retries}
  standby_gsm:
    container_name: ${STANDBY_GSM_CONTAINER_NAME}
    privileged: ${CONTAINER_PRIVILEGED_FLAG}
    hostname: ${STANDBY_GSM_HOSTNAME}
    image: ${GSM_IMAGE}
    dns_search: ${DNS_SEARCH}
    restart: ${CONTAINER_RESTART_POLICY}
    volumes:
      - ${DOCKERVOLLOC}/dbfiles/GSM2DATA:/opt/oracle/gsmdata
      - /opt/.secrets:/run/secrets:ro
      - /opt/containers/shard_host_file:/etc/hosts
    environment:
      SHARD_SETUP: ${STANDBY_GSM_SHARD_SETUP}
      DOMAIN: ${DOMAIN}
      SHARD_DIRECTOR_PARAMS: ${STANDBY_SHARD_DIRECTOR_PARAMS}
      SHARD1_GROUP_PARAMS: ${STANDBY_SHARD1_GROUP_PARAMS}
      CATALOG_PARAMS: ${STANDBY_CATALOG_PARAMS}
      SHARD1_PARAMS: ${STANDBY_SHARD1_PARAMS}
      SHARD2_PARAMS: ${STANDBY_SHARD2_PARAMS}
      SHARD3_PARAMS: ${STANDBY_SHARD3_PARAMS}
      SERVICE1_PARAMS: ${STANDBY_SERVICE1_PARAMS}
      SERVICE2_PARAMS: ${STANDBY_SERVICE2_PARAMS}
      COMMON_OS_PWD_FILE: ${COMMON_OS_PWD_FILE}
      PWD_KEY: ${PWD_KEY}
      OP_TYPE: ${GSM_OP_TYPE}
    networks:
      shard_pub1_nw:
        ipv4_address: ${LOCAL_NETWORK}.154
    depends_on:
      primary_gsm:
        condition: service_healthy
```

## Create services using "docker compose" command
Once you have completed the prerequisties, run below commands to create the services:
```
# Switch to location with the "docker-compose.yaml" file and run:
 
docker compose up -d

``` 

## Check the logs

```
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

```
docker-compose down
```

## Copyright

Copyright (c) 2022, 2023 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/