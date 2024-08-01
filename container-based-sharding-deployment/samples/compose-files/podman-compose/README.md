# Deploying Oracle Globally Distributed Database Containers using podman-compose
For Oracle Linux 8 host machines,`podman-compose` can be used for deploying containers to create an Oracle Globally Distributed database. 

You can use Oracle 23ai GSM and RDBMS Podman Images(Enterprise or FREE) and can deploy with `System-Managed Sharding` or `System-Managed Sharding Topology with Raft replication` or `User Defined Sharding` feature while deploying the Oracle Globally Distributed Database.

Below steps provide an example to `podman-compose` to create the podman network and deploy containers for a Oracle Globally Distributed Database on a single Oracle Linux 8 host.

This example deploys an Oracle Globally Distributed Database with `System-Managed Sharding Topology with Raft replication` using Oracle 23ai GSM and RDBMS Images with Four shard containers, a Catalog Container, a Primary GSM container and a Standby GSM Container.

**IMPORTANT:** This example uses 23ai RDBMS and 23ai GSM Podman Images. 

**IMPORTANT:** Also, this example enables the RAFT Replication feature while deploying the Oracle Globally Distributed database. 

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

### Create Podman Secrets

Complete steps to create podman secrets from [Password Management](../../container-files/podman-container-files/README.md#password-management). Same podman secrets are going to be used during Oracle Globally Distributed Database Containers.

### Prerequisites script file
Use the script file [podman-compose-prequisites.sh](./podman-compose-prequisites.sh) to export the environment variables, create the network host file and create required directories before running next steps:

**NOTE:** You will need to change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the images you want to use for the deployment.

```bash
source podman-compose-prequisites.sh
```

## SELinux Configuration Management for Podman Host
If SELinux is enabled on podman-host then load the necessary `shard-podman` policy as explained in [SELinux Configuration on Podman Host](../container-files/podman-container-files/README.md#selinux-configuration-on-podman-host)

Additionally, execute below command to set SELinux contexts for required files and folders using the file [set-file-context.sh](./set-file-context.sh)
```bash
source set-file-context.sh
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

- With the environment variables set in [Prerequisites Section](#complete-the-prerequisite-steps), execute the below command to remove the Oracle Globally Distributed Database Containers and folders:

```bash
podman-compose down
rm -rf ${PODMANVOLLOC}
```

## Oracle 23ai FREE Database and GSM Images

In case you are going to use Oracle 23ai FREE Database and GSM Images for deploying the Oracle Globally Distributed Database, then you need to:

- Use file [podman-compose-prequisites-free.sh](./podman-compose-prequisites-free.sh) as the prerequisites script file before running the above setup.

**NOTE:** You will need to change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the Oracle 23ai FREE Images you want to use for the deployment.

- Take the file [podman-compose-free.yml](./podman-compose-free.yml) and rename as `podman-compose.yml` to deploy the setup using `podman-compose` command.

## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/