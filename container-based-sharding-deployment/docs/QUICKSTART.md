# Oracle Sharding Container QuickStart Guide - Oracle 23ai   

This quickstart aims to help you install Oracle Sharding Containers with SNR RAFT feature enabled on a Single host Oracle Linux Host machine using Podman Compose, Oracle Sharding Container Image and bridge network driver for Podman.

- [Oracle Sharding Container QuickStart Guide - Oracle 23ai](#oracle-sharding-container-quickstart-guide---oracle-23ai)
  - [Before you begin](#before-you-begin)
  - [Getting Oracle Sharding Images](#getting-oracle-rac-images)
  - [Networking in Oracle Sharding Podman Container Environment](#networking-in-oracle-rac-podman-container-environment)
  - [Deploy Oracle Sharding 2 Node Environment with NFS Storage Container](#deploy-oracle-rac-2-node-environment-with-nfs-storage-container)
  - [Deploy Oracle Sharding 2 Node Environment with BlockDevices](#deploy-oracle-rac-2-node-environment-with-blockdevices)
  - [Validating Oracle Sharding Environment](#validating-oracle-rac-environment)
  - [Connecting Oracle Sharding Environment](#connecting-to-oracle-rac-environment)
  - [Environment Variables Explained for above 2 Node RAC on Podman Compose](#environment-variables-explained-for-above-2-node-rac-on-podman-compose)
  - [Support](#support)
  - [License](#license)
  - [Copyright](#copyright)

## Before you begin
- Before proceeding further, prepate podman host with  prerequisites related to the Oracle Sharding Containers on Podman host Environment as explained in [Preparation Steps for running Oracle Sharding Database in containers](../README.md#preparation-steps-for-running-oracle-sharding-in-linux-containers). We have a pre-created script `setup_sharding_host.sh` which will prepare the podman host with the following pre-requisites-
    - Validate Host machine for supported Os version(OL >8), Kernel(>UEKR6), Memory(>32GB), etc.
    - Install Podman
    - Install Podman Compose
    - Setup and Load SELinux modules
    - Create Oracle Sharding Podman secrets
- Set `secret-password` of your choice below, which is going to be used as a password for the Oracle Sharding Container environment. 
  Execute below command- 
  ```bash
  export SHARDING_SECRET=<secret-password>
  ``` 
- To prepare podman host machine using a pre-created script, copy the file `setup_sharding_host.sh` from [<GITHUB_REPO_CLONED_PATH>/db-sharding/container-based-sharding-deployment/containerfiles/setup_sharding_host.sh](../containerfiles/setup_sharding_host.sh) and execute below -
  ```bash
  ./setup_sharding_host.sh -prepare-sharding-env
  ```
  Logs-
  ```bash
  INFO: Finished setting up the pre-requisites for Podman-Host
  ``` 
- In this quickstart, our working directory is `<GITHUB_REPO_CLONED_PATH>/db-sharding/container-based-sharding-deployment/containerfiles` from where all commands are executed.

## Getting Oracle Sharding Images
- Refer to the [Getting Oracle Sharding Database Container Images](../README.md#building-oracle-sharding-container-images) section to get Oracle Sharding Images used in quickstart setup.  
- When Podman Images are ready like the below example used in this quickstart, you can proceed to the next steps-
  ```bash
  podman images
  REPOSITORY                                               TAG                 IMAGE ID      CREATED       SIZE
  localhost/oracle/database-ext-sharding                   23.4.0-ee           1c17b71b710e  21 hours ago  7.06 GB
  localhost/oracle/database                                23.4.0-ee           ee6a794351a0  22 hours ago  6.74 GB
  localhost/oracle/gsm                                     23.4.0              8721eee1dbba  23 hours ago  2.14 GB
  localhost/oracle/database                                23.4.0-ee-base      b40d790f3224  8 days ago    339 MB
  localhost/oracle/database-gsm                            23.4.0              e53dc906f121  8 days ago    2.1 GB
  ```
## Networking in Oracle Sharding Podman Container Environment
- In this Quick Start, we will create below subnets for Oracle Sharding Podman Container Environment-  

  | Network Name   | Subnet CIDR         | Description                          |
  |----------------|--------------|--------------------------------------|
  | shard_pub1_nw    | 10.0.20.0/20 | Public network for Oracle Sharding Podman Container Environment                      |

## Deploy Oracle Sharding Environment
- Copy `podman-compose.yml` file from this [<GITHUB_REPO_CLONED_PATH>/db-sharding/container-based-sharding-deployment/samples/podman-compose.yml](../samples/compose-files/podman-compose.yml) in your working directory.
- Execute the below command from your working directory to export the required environment variables required by the compose file in this quickstart-
  ```bash
  source ./setup_sharding_host.sh -export-sharding-env
  ```
  Logs -
  ```bash
  INFO: Sharding Environment Variables are setup successfully.
  ```
- Execute the below command from your working directory to setup pre-requisites in this quickstart-
  ```bash
  source ./setup_sharding_host.sh -prepare-sharding-env
  ```
  Logs -
  ```bash
  INFO: Finished setting up the pre-requisites for Podman-Host
  ```
- Execute below to deploy Catalog Container-
  ```bash
  ./setup_sharding_host.sh -deploy-catalog
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f catalog_db
  ==============================================
     GSM Catalog Setup Completed              
  ==============================================
  ```
- Execute below to deploy Shard1 Container-
  ```bash
  ./setup_sharding_host.sh -deploy-shard1
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f shard1_db
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- Execute below to deploy Shard2 Container-
  ```bash
  ./setup_sharding_host.sh -deploy-shard2
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f shard2_db
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- Execute below to deploy Shard3 Container-
  ```bash
  ./setup_sharding_host.sh -deploy-shard3
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f shard3_db
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- Execute below to deploy Shard4 Container-
  ```bash
  ./setup_sharding_host.sh -deploy-shard4
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f shard4_db
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- Execute below to deploy Primary GSM Container-
  ```bash
  ./setup_sharding_host.sh -deploy-gsm-primary
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f primary_gsm
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- Execute below to deploy Standby GSM Container-
  ```bash
  ./setup_sharding_host.sh -deploy-gsm-standby
  ```
  Monitor Logs -
  ```bash
  podman-compose logs -f standby_gsm
  ==============================================
     GSM Shard Setup Completed                
  ==============================================
  ```
- If you want to cleanup the Sharding Container environment, then execute below-
  ```bash
  ./setup_sharding_host.sh -cleanup
  ```
  This will cleanup Oracle Sharding Containers, Oracle Storage Volume,  Oracle Sharding Podman Networks, etc.

  Logs-
  ```bash
  INFO: Oracle Container RAC Environment Cleanup Successfully
  ```


## Validating Oracle Sharding Environment
You can validate if the environment is setup properly by running the below command and checking logs of each container-
```bash
podman ps -a
CONTAINER ID  IMAGE                                             COMMAND               CREATED         STATUS         PORTS       NAMES
181a4215b517  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  22 minutes ago  Up 22 minutes              catalog
2b5ade918112  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  18 minutes ago  Up 18 minutes              shard1
f4943c6d67ce  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  15 minutes ago  Up 15 minutes              shard2
fa9611ad2bbe  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  11 minutes ago  Up 11 minutes              shard3
e19138866b51  localhost/oracle/database-ext-sharding:23.4.0-ee  /bin/sh -c exec $...  7 minutes ago   Up 7 minutes               shard4
e88e57c4e442  localhost/oracle/database-gsm:23.4.0              /bin/sh -c exec $...  3 minutes ago   Up 3 minutes               gsm1
4e751cdfe01e  localhost/oracle/database-gsm:23.4.0              /bin/sh -c exec $...  24 seconds ago  Up 24 seconds              gsm2
```

## Environment Variables Explained
Refer to [Environment Variables Explained for Oracle Sharding on Podman Compose](./ENVVARIABLESCOMPOSE.md) for the explanation of all the environment variables related to Oracle Sharding on Podman Compose. Change or Set these environment variables as per your environment.

## Support

At the time of this release, Oracle Sharding on Podman is supported for Oracle Linux 9.3 later. To see current Linux support certifications, refer [Oracle Sharding on Podman Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/21/install-and-upgrade.html)

## License

To download and run Oracle Grid and Database, regardless of whether inside or outside a container, you must download the binaries from the Oracle website and accept the license indicated on that page.

All scripts and files hosted in this repository that are required to build the container images are, unless otherwise noted, released under a UPL 1.0 license.

## Copyright

Copyright (c) 2014-2024 Oracle and/or its affiliates.




