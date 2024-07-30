# Deploying Sharding Containers on podman-compose
For Oracle Linux 8 host machines,`podman-compose` can be used for deploying containers to create an Oracle Sharded database. 

You can use Oracle 23ai GSM and RDBMS Podman Images(Enterprise or FREE) and can deploy with `System-Managed Sharding` or `System-Managed Sharding Topology with Raft replication` or `User Defined Sharding` feature while deploying the Oracle Globally Distributed Database.

Below steps provide an example to `podman-compose` to create the podman network and deploy containers for a Globally Distributed Database on a single Oracle Linux 8 host.

This example deploys an Oracle Globally Distributed Database with `System-Managed Sharding Topology with Raft replication` using Oracle 23ai GSM and RDBMS Images with Four shard containers, a Catalog Container, a Primary GSM container and a Standby GSM Container.

**IMPORTANT:** This example uses 23ai RDBMS and 23ai GSM Podman Images. 

**IMPORTANT:** Also, this example enables the SNR RAFT feature while deploying the Sharded Database. 

- [Step 1: Install Podman compose](#install-podman-compose)
- [Step 2: Complete the prerequisite steps](#complete-the-prerequisite-steps)
- [Step 3: SELinux Configuration Management for Podman Host](#selinux-configuration-management-for-podman-host)
- [Step 4: Create Podman Compose file](#create-podman-compose-file)
- [Step 5: Create services using "podman-compose" command](#create-services-using-podman-compose-command)
- [Step 6: Check the logs](#check-the-logs)
- [Step 7: Remove the deployment](#remove-the-deployment)
- [Step 8: Oracle 23ai FREE Database and GSM Images](#oracle-23ai-free-database-and-gsm-images)
- [Copyright](#copyright)


## Install Podman compose
```bash
dnf config-manager --enable ol8_developer_EPEL
dnf install podman-compose
```

## Complete the prerequisite steps

Use the file [podman-compose-env-variables](./podman-compose-env-variables) to export the environment variables before running next steps:

**NOTE:** You will need to change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the images you want to use for the deployment.

```bash
source podman-compose-env-variables
```

Use below steps to create a network host file:

```bash
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
```

Use below steps to create an encrypted password file:

```bash
rm -rf /opt/.secrets/
mkdir /opt/.secrets/
openssl genrsa -out /opt/.secrets/key.pem
openssl rsa -in /opt/.secrets/key.pem -out /opt/.secrets/key.pub -pubout

# Edit the file /opt/.secrets/pwdfile.txt to add the password string
vi /opt/.secrets/pwdfile.txt

# Encrypt the file having the password
openssl pkeyutl -in /opt/.secrets/pwdfile.txt -out /opt/.secrets/pwdfile.enc -pubin -inkey /opt/.secrets/key.pub -encrypt

rm -f /opt/.secrets/pwdfile.txt

podman secret create pwdsecret /opt/.secrets/pwdfile.enc
podman secret create keysecret /opt/.secrets/key.pem
```

Create required directories:
```bash
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

## Create Podman Compose file

In this step, copy the [podman-compose.yml](podman-compose.yml) in your working directory. In this guide, our working directory is [<github_cloned_path>/db-sharding/container-based-sharding-deployment/containerfiles]

## Create services using "podman-compose" command
Once you have completed the prerequisties, run below commands to create the services:
```bash
# Ensure "podman-compose.yml" file is present in your working directory and then execute below:
 
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

## Oracle 23ai FREE Database and GSM Images

In case you are going to use Oracle 23ai FREE Database and GSM Images for deploying the Oracle Globally Distributed Database, then you need to:
- Use file [podman-compose-env-variables-free](./podman-compose-env-variables-free) to export the environment variables before running the above setup.

**NOTE:** You will need to change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the images you want to use for the deployment.

- Take the file [podman-compose-free.yml](./podman-compose-free.yml) and rename as `podman-compose.yml` to deploy the setup using `podman-compose` command.

## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/