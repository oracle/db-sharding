# Deploying Oracle Globally Distributed Database Containers using podman-compose
For Oracle Linux 8 host machines,`podman-compose` can be used for deploying containers to create an Oracle Globally Distributed Database. 

You can use Oracle 23ai GSM and RDBMS Podman Images (Enterprise or FREE) and deploy with the sharding option of your choice: `System-Managed Sharding`, `System-Managed Sharding Topology with Raft replication`, or `User Defined Sharding`. The sharding optoin is deployed with Oracle Globally Distributed Database.

The example that follows shows how to use `podman-compose` to create the Podman network and to deploy containers for an Oracle Globally Distributed Database on a single Oracle Linux 8 host.

In this example, we deploy an Oracle Globally Distributed Database with `System-Managed Sharding Topology with Raft replication` using Oracle 23ai GSM and RDBMS Images with Four shard containers, a Catalog Container, a Primary GSM container, and a Standby GSM Container.

**IMPORTANT:** This example uses 23ai RDBMS and 23ai GSM Podman Images, and enables the RAFT Replication feature while deploying the Oracle Globally Distributed database. 


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
Complete each of these steps before proceeding with deployment. 

### Create Podman Secrets

Complete the procedure to create Podman secrets from [Password Management](../../container-files/podman-container-files/README.md#password-management). These Podman secrets are also used during the deployment of Oracle Globally Distributed Database Containers.

### Prerequisites script file
Run the script file [podman-compose-prequisites.sh](./podman-compose-prequisites.sh). This script exports the environment variables, creates the network host file, and creates required directories.

**NOTE:** You must change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the images that you want to use for the deployment.

```bash
source podman-compose-prequisites.sh
```

## SELinux Configuration Management for Podman Host
If SELinux is enabled on your podman-host, then load the necessary `shard-podman` policy, as explained in [SELinux Configuration on Podman Host](../container-files/podman-container-files/README.md#selinux-configuration-on-podman-host)

To set SELinux contexts for required files and folders, run the file [set-file-context.sh](./set-file-context.sh)
```bash
source set-file-context.sh
```

## Create Podman Compose file

Copy the [podman-compose.yml](podman-compose.yml) into your working directory. In this example, our working directory is [<github_cloned_path>/db-sharding/container-based-sharding-deployment/containerfiles]

## Create services using "podman-compose" command
After you have completed all the prerequisties successfully, run the following command to create the services: 
```bash
# Ensure "podman-compose.yml" file is present in your working directory and then run the following command:
 
podman-compose up -d
```

Wait for all the services setup to be complete and ready:
```bash
podman ps -a
CONTAINER ID  IMAGE                                             COMMAND               CREATED        STATUS        PORTS       NAMES
56fd25ab3476  localhost/oracle/database-ext-sharding:23.5.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              catalog
05e1d72ae93e  localhost/oracle/database-ext-sharding:23.5.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard1
7dbd9ce5564b  localhost/oracle/database-ext-sharding:23.5.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard2
5e1341e3eeab  localhost/oracle/database-ext-sharding:23.5.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard3
dad4f89a8aaa  localhost/oracle/database-ext-sharding:23.5.0-ee  /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              shard4
a265f8438bb7  localhost/oracle/database-gsm:23.5.0              /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              gsm1
f155a7f61830  localhost/oracle/database-gsm:23.5.0              /bin/sh -c exec $...  7 minutes ago  Up 7 minutes              gsm2
```

## Check the logs
```bash
# You can monitor the logs for all the containers using the following command:
 
podman-compose logs -f
```
Look for successful message in all containers. For example:-
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

If you want to remove the deployment, then run the `podman-compose` command. To remove the deployment:

- With the environment variables set in [Prerequisites Section](#complete-the-prerequisite-steps), run the following command to remove the Oracle Globally Distributed Database Containers and folders:

```bash
podman-compose down
rm -rf ${PODMANVOLLOC}
```

## Oracle 23ai FREE Database and GSM Images

If you plan to use Oracle 23ai FREE Database and GSM Images for deploying the Oracle Globally Distributed Database, then complete these steps:

- Use file [podman-compose-prequisites-free.sh](./podman-compose-prequisites-free.sh) as the prerequisites script file before running the setup as described above.

**NOTE:** You must change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the Oracle 23ai FREE Images you want to use for the deployment.

- Take the file [podman-compose-free.yml](./podman-compose-free.yml) and rename it as `podman-compose.yml` to deploy the setup using the `podman-compose` command.

## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/