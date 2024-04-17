# Deploying Sharding Containers on podman-compose
For Oracle Linux 8 host machines,`podman-compose` can be used for deploying containers to create an Oracle Sharded database. 

You can use Oracle 23ai GSM and RDBMS Podman Images and can enable the `SNR RAFT` feature while deploying the Oracle Sharded Database.

Below steps provide an example to `podman-compose` to create the podman network and deploy containers for a Sharded Database on a single Oracle Linux 8 host. 

This example deploys an Oracle Sharded Database with SNR RAFT using Oracle 23ai GSM and RDBMS Images with Four shard containers, a Catalog Container, a Primary GSM container and a Standby GSM Container.

**IMPORTANT:** This example uses 23ai RDBMS and 23ai GSM Podman Images. 

**IMPORTANT:** Also, this example enables the SNR RAFT feature while deploying the Sharded Database. 

- [Step 1: Install Podman compose](#install-podman-compose)
- [Step 2: Complete the prerequisite steps](#complete-the-prerequisite-steps)
- [Step 3: SELinux Configuration Management for Podman Host](#selinux-configuration-management-for-podman-host)
- [Step 4: Password Management](#password-management)
- [Step 5: Create Podman Compose file](#create-podman-compose-file)
- [Step 6: Create services using "podman-compose" command](#create-services-using-podman-compose-command)
- [Step 7: Check the logs](#check-the-logs)
- [Step 8: Remove the deployment](#remove-the-deployment)
- [Copyright](#copyright)


## Install Podman compose
```bash
dnf config-manager --enable ol8_developer_EPEL
dnf install podman-compose
```

## Complete the prerequisite steps

Run the below set of commands to export variables and do folder creations as part of pre-requisites:
```bash
    export PODMANVOLLOC='/scratch/oradata'
    export NETWORK_INTERFACE='ens3'
    export NETWORK_SUBNET="10.0.20.0/20"
    export SIDB_IMAGE='oracle/database-ext-sharding:23.4.0-ee'
    export GSM_IMAGE='oracle/database-gsm:23.4.0'
    export LOCAL_NETWORK=10.0.20
    export healthcheck_interval=30s
    export healthcheck_timeout=3s
    export healthcheck_retries=40
    export CATALOG_OP_TYPE="catalog"
    export ALLSHARD_OP_TYPE="primaryshard"
    export GSM_OP_TYPE="gsm"
    export PWD_SECRET_FILE=/opt/.secrets/pwdfile.enc
    export KEY_SECRET_FILE=/opt/.secrets/key.pem
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

    export SHARD3_CONTAINER_NAME="shard3"
    export SHARD3_HOSTNAME="oshard3-0"
    export SHARD3_CDB="ORCL3CDB"
    export SHARD3_PDB="ORCL3PDB"

    export SHARD4_CONTAINER_NAME="shard4"
    export SHARD4_HOSTNAME="oshard4-0"
    export SHARD4_CDB="ORCL4CDB"
    export SHARD4_PDB="ORCL4PDB"

    export PRIMARY_GSM_CONTAINER_NAME="gsm1"
    export PRIMARY_GSM_HOSTNAME="oshard-gsm1"
    export STANDBY_GSM_CONTAINER_NAME="gsm2"
    export STANDBY_GSM_HOSTNAME="oshard-gsm2"


    export PRIMARY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector1;director_region=region1;director_port=1522"
    export PRIMARY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=primary;group_region=region1"
    export PRIMARY_CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2;catalog_chunks=30;repl_type=Native"    
    export PRIMARY_SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"
    export PRIMARY_SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"
    export PRIMARY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=primary"
    export PRIMARY_SERVICE2_PARAMS="service_name=oltp_rw_svc;service_role=primary"

    export STANDBY_SHARD_DIRECTOR_PARAMS="director_name=sharddirector2;director_region=region1;director_port=1522   "
    export STANDBY_SHARD1_GROUP_PARAMS="group_name=shardgroup1;deploy_as=active_standby;group_region=region1"
    export STANDBY_CATALOG_PARAMS="catalog_host=oshard-catalog-0;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2;catalog_chunks=30;repl_type=Native"
    export STANDBY_SHARD1_PARAMS="shard_host=oshard1-0;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SHARD2_PARAMS="shard_host=oshard2-0;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SHARD3_PARAMS="shard_host=oshard3-0;shard_db=ORCL3CDB;shard_pdb=ORCL3PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SHARD4_PARAMS="shard_host=oshard4-0;shard_db=ORCL4CDB;shard_pdb=ORCL4PDB;shard_port=1521;shard_group=shardgroup1"
    export STANDBY_SERVICE1_PARAMS="service_name=oltp_rw_svc;service_role=standby"
    export STANDBY_SERVICE2_PARAMS="service_name=oltp_ro_svc;service_role=standby"

    mkdir -p  /opt/containers
    rm -f /opt/containers/shard_host_file && touch /opt/containers/shard_host_file
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
    mkdir -p ${PODMANVOLLOC}/scripts
    chown -R 54321:54321 ${PODMANVOLLOC}/scripts
    chmod 755 ${PODMANVOLLOC}/scripts

    mkdir -p ${PODMANVOLLOC}/dbfiles/CATALOG
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/CATALOG

    mkdir -p ${PODMANVOLLOC}/dbfiles/ORCL1CDB
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/ORCL1CDB
    mkdir -p ${PODMANVOLLOC}/dbfiles/ORCL2CDB
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/ORCL2CDB
    mkdir -p ${PODMANVOLLOC}/dbfiles/ORCL3CDB
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/ORCL3CDB
    mkdir -p ${PODMANVOLLOC}/dbfiles/ORCL4CDB
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/ORCL4CDB

    mkdir -p ${PODMANVOLLOC}/dbfiles/GSMDATA
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/GSMDATA

    mkdir -p ${PODMANVOLLOC}/dbfiles/GSM2DATA
    chown -R 54321:54321 ${PODMANVOLLOC}/dbfiles/GSM2DATA

    chmod 755 ${PODMANVOLLOC}/dbfiles/CATALOG
    chmod 755 ${PODMANVOLLOC}/dbfiles/ORCL1CDB
    chmod 755 ${PODMANVOLLOC}/dbfiles/ORCL2CDB
    chmod 755 ${PODMANVOLLOC}/dbfiles/ORCL3CDB
    chmod 755 ${PODMANVOLLOC}/dbfiles/ORCL4CDB
    chmod 755 ${PODMANVOLLOC}/dbfiles/GSMDATA
    chmod 755 ${PODMANVOLLOC}/dbfiles/GSM2DATA
```
## SELinux Configuration Management for Podman Host
If SELinux is enabled on podman-host then load the necessary `rac-podman` policy as explained in [SELinux Configuration on Podman Host](../container-files/podman-container-files/README.md#selinux-configuration-on-podman-host)

Additionally, execute below command to set SELinux contexts for required files and folders-
```bash
files=(
        "${PODMANVOLLOC}/dbfiles/CATALOG"
        "/opt/containers/shard_host_file"
        "${PODMANVOLLOC}/dbfiles/ORCL1CDB"
        "${PODMANVOLLOC}/dbfiles/ORCL2CDB"
        "${PODMANVOLLOC}/dbfiles/GSMDATA"
        "${PODMANVOLLOC}/dbfiles/GSM2DATA"
        "${PODMANVOLLOC}/dbfiles/ORCL3CDB"
        "${PODMANVOLLOC}/dbfiles/ORCL4CDB"
    )

    # Check if SELinux is enabled (enforcing or permissive)
    if grep -q '^SELINUX=enforcing' /etc/selinux/config || grep -q '^SELINUX=permissive' /etc/selinux/config; then
        for file in "${files[@]}"; do
            semanage fcontext -a -t container_file_t "$file"
            restorecon -v "$file"
        done
        echo "SELinux is enabled. Updated file contexts."
    fi
```
## Password Management
Complete steps to create podman secrets from [Password Management](../container-files/podman-container-files/README.md#password-management). Same podman secrets are going are going to be used during Oracle Sharding Containers.

## Create Podman Compose file

In this step, copy the [podman-compose.yml](podman-compose.yml) in your working directory. In this guide, our working directory is [<github_cloned_path>/db-sharding/container-based-sharding-deployment/containerfiles]

## Create services using "podman-compose" command
Once you have completed the prerequisties, run below commands to create the services:
```bash
# Ensure "podman-compose.yaml" file is present in your working directory and then execute below:
 
podman-compose up -d
```

Wait for all setup to be ready:
```bash
podman ps -a
CONTAINER ID  IMAGE                                             COMMAND               CREATED        STATUS        PORTS       NAMES
56fd25ab3476  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              catalog
05e1d72ae93e  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard1
7dbd9ce5564b  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard2
5e1341e3eeab  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard3
dad4f89a8aaa  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard4
a265f8438bb7  localhost/oracle/database-gsm:23.4.0              /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              gsm1
f155a7f61830  localhost/oracle/database-gsm:23.4.0              /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              gsm2
```

## Check the logs
```bash
# You can monitor the logs for all the containers using below command:
 
podman-compose logs -f
```
Look for successful message in all containers-
```bash
podman logs -f catalog
==============================================
         GSM Catalog Setup Completed
==============================================

podman logs -f shard1
==============================================
     GSM Shard Setup Completed                
==============================================

podman logs -f shard2
==============================================
     GSM Shard Setup Completed                
==============================================

podman logs -f gsm1
==============================================
     GSM Setup Completed                      
==============================================

podman logs -f gsm2
==============================================
     GSM Setup Completed
==============================================
```

## Remove the deployment

You can also use the `podman-compose` command to remove the deployment. To remove the deployment:

- First export all the variables from the [Prerequisites Section](#complete-the-prerequisite-steps)
- Execute the below command to remove the Oracle Sharding Containers and folders:

```bash
podman-compose down
rm -rf ${PODMANVOLLOC}
```

## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/