# Oracle Sharding in Linux Containers

Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

This project offers sample container files to facilitate installation, configuration, and environment setup for DevOps users. For more information about Oracle Database please see the [Oracle Sharded Database Management Documentation](http://docs.oracle.com/en/database/).

## How to build and run

Please review README of following sections in a given order. After reviewing the README of each section, you can skip the image/container creation if you do not meet the requirement.

* Please review following points before you proceed to next sections:
  * Install Docker if using Oracle Linux 7
    * You must install [Docker Engine](https://docs.oracle.com/en/operating-systems/oracle-linux/docker/) if you are using Oracle Linux 7 system.
    * You need to install "docker-engine" and "docker-cli" using yum command.
  * Install Podman if using Oracle Linux 8
    * You must install and configure [Podman release 4.0.2](https://docs.oracle.com/en/operating-systems/oracle-linux/Podman/) or later on Oracle Linux 8.5 or later to run Oracle Sharding on Podman.
    * You need to install `podman-docker` utility using dnf command.

This project offers sample container files for:

* Oracle Database 23c Global Service Manager (GSM/GDS) (23.3) for Linux x86-64
* Older Releases: Oracle 19c (19.3) and Oracle 21c (21.3) for Linux x86-64

To assist in building the images, you can use the [buildContainerImage.sh](dockerfiles/buildContainerImage.sh) script.See section **Create Oracle Global Service Manager Image** for instructions and usage.

**IMPORTANT:** Oracle Global Service Manager container is useful when you want to configure the Global Data Service Framework. The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases.

## Using GSM and Sharding Image

To deploy a Oracle Sharding topology, please execute the steps in the following sections below:
- [Oracle Sharding in Linux Containers](#oracle-sharding-in-linux-containers)
  - [How to build and run](#how-to-build-and-run)
  - [Using GSM and Sharding Image](#using-gsm-and-sharding-image)
    - [Create Oracle Global Service Manager Image](#create-oracle-global-service-manager-image)
    - [Create Oracle Database Image](#create-oracle-database-image)
    - [Create Extended Oracle Database Image with Sharding Feature](#create-extended-oracle-database-image-with-sharding-feature)
  - [Create Containers using compose files](#create-containers-using-compose-files)
    - [Create Containers using docker compose](#create-containers-using-docker-compose)
    - [Create Containers using podman-compose](#create-containers-using-podman-compose)
  - [Create Containers using manual steps](#create-containers-using-manual-steps)
    - [Create Containers manually using Docker](#create-containers-manually-using-docker)
    - [Create Containers manually using Podman](#create-containers-manually-using-podman)
  - [Support](#support)
  - [License](#license)
  - [Copyright](#copyright)

### Create Oracle Global Service Manager Image

**IMPORTANT:** You will have to provide the installation binaries of Oracle Global Service Manager Oracle Database 23c  (23.3) for Linux x86-64 and put them into the `dockerfiles/<version>` folder. You only need to provide the binaries for the edition you are going to install. The binaries can be downloaded from the [Oracle Technology Network](http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). You also have to make sure to have internet connectivity for yum.
**Note:** You must not uncompress the binaries.

The `buildContainerImage.sh` script is just a utility shell script that performs MD5 checks and is an easy way for beginners to get started. Expert users are welcome to directly call `docker build` with their preferred set of parameters. Before you build the image make sure that you have provided the installation binaries and put them into the right folder. Go into the **dockerfiles** folder and run the **buildContainerImage.sh** the script as root or with sudo privileges:

```
./buildContainerImage.sh -v (Software Version)
./buildContainerImage.sh -v 23.3.0
```

For detailed usage of command, please execute following command:

```
./buildContainerImage.sh -h
```

### Create Oracle Database Image

To build Oracle Sharding on docker/container, you need to download and build Oracle 23.3 Database image, please refer [README.MD](https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md) of Oracle Single Database available on Oracle GitHub repository.

**Note**: You just need to create the image as per the instructions given in [README.MD](https://github.com/oracle/docker-images/blob/master/OracleDatabase/SingleInstance/README.md) but you will create the container as per the steps given in this document under [Create Containers](#create-containers) section.

### Create Extended Oracle Database Image with Sharding Feature

After creating the base image using buildContainerImage.sh in the previous step, use buildExtensions.sh present under the extensions folder to build an extended image that will include the Sharding Feature. Please refer [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/extensions/README.md) of extensions folder of Oracle Single Database available on Oracle GitHub repository.

For example:

```
./buildExtensions.sh -a -x sharding -b oracle/database:23.3.0-ee  -t oracle/database-ext-sharding:23.3.0-ee

Where:
"-x sharding"                                   is to specify to have sharding feature in the extended image
"-b oracle/database:23.3.0-ee"                  is to specify the Base image created in previous step
"oracle/database-ext-sharding:23.3.0-ee"        is to specify the name:tag for the extended image with Sharding Feature
```

## Create Containers using compose files

Please check the below sections to use compose files to deploy containers for the sharded database in an automated way.

### Create Containers using docker compose

[Compose](https://docs.docker.com/compose/) is a tool for defining and running multi-container Docker applications. With "docker-compose", you use a YAML file to configure your application's services. Then, with a single command, you create and start all the services from your configuration. Please check [sample deployment using docker compose](./samples/compose-files/docker-compose.md) to deploy an Oracle Sharded Database on an Oracle Linux 7 Machine using Oracle 21c RDBMS and 21c GSM Docker Images.


### Create Containers using podman-compose

For Oracle Linux 8 host machines,"podman-compose" can be used for deploying containers to create an Oracle Sharded database. You can use Oracle 23c GSM and RDBMS Podman Images and can enable the SNR RAFT feature while deploying the Oracle Sharded Database. Please check [sample deployment using podman-compose](./samples/compose-files/podman-compose.md) to deploy an Oracle Sharded Database on an Oracle Linux 8 Machine using Oracle 23c RDBMS and 23c GSM Podman Images.




## Create Containers using manual steps

In case, you want to manually deploy the sharded database using Docker or Podman containers, please refer to the below sections for the step by step procedure.

### Create Containers manually using Docker

Please refer to [Manually deploy a Sharded Database using Docker](./samples/manual-steps/docker-manual-steps/README.md) to deploy a  sharded database. This document provides the commands to deploy a Sharded Database using Docker with System Sharding or with User Defined Sharding.

**NOTE:** If you want to use Oracle 19c or Oracle 21c version based container images with Docker, you need to deploy on an Oracle Linux 7 host.

### Create Containers manually using Podman

Please refer to [Manually deploy a Sharded Database using Podman](./samples/manual-steps/podman-manual-steps/README.md) to deploy a  sharded database. This document provides the commands to deploy a Sharded Database using Podman with System Sharding or with User Defined Sharding.

**NOTE:** If you want to use Oracle 21c or Oracle 23c version based container images with Podman, you need to deploy on an Oracle Linux 8 host.


## Support

Oracle GSM and Sharding Database is supported for Oracle Linux 7. Oracle 23c GSM is supported on Oracle Linux 8.


## License

To download and run Oracle GSM and Sharding Database, regardless whether inside or outside a Docker container, you must download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker images are, unless otherwise noted, released under UPL 1.0 license.


## Copyright

Copyright (c) 2014-2022 Oracle and/or its affiliates.