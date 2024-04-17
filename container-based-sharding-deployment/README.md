# Oracle Sharding in Linux Containers

Learn about container deployment options for Oracle Sharding in Linux Containers Release 23ai (v23.4)

## Overview of Oracle Sharding in Linux Containers

Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

This project offers sample container files to facilitate installation, configuration, and environment setup for DevOps users. For more information about Oracle Database please see the [Oracle Sharded Database Management Documentation](http://docs.oracle.com/en/database/).

Please review README of following sections in a given order. After reviewing the README of each section, you can skip the image/container creation if you do not meet the requirement.

This project offers sample container files for:

* Oracle Database 23ai Global Service Manager (GSM/GDS) (23.4.0) for Linux x86-64
* Older Releases: Oracle 19c (19.3) and Oracle 21c (21.3) for Linux x86-64


## Using this Documentation
To create an Oracle Sharding Container environment, follow these steps:

- [Oracle Sharding in Linux Containers](#oracle-sharding-in-linux-containers)
  - [Overview of Oracle Sharding in Linux Containers](#overview-of-oracle-sharding-in-linux-containers)
  - [Using this Documentation](#using-this-documentation)
  - [QuickStart](#quickstart)
  - [Preparation Steps for running Oracle Sharding in Linux Containers](#preparation-steps-for-running-oracle-rac-database-in-containers)
  - [Building Oracle Sharding Container Images](#building-oracle-sharding-container-images)
    - [Building Oracle Global Service Manager Image](#building-oracle-global-service-manager-image)
    - [Building Oracle Database Image](#building-oracle-database-image)
    - [Building Extended Oracle Database Image with Sharding Feature](#building-extended-oracle-database-image-with-sharding-feature)
  - [Oracle Sharding in Containers Deployment Scenarios](#oracle-sharding-in-containers-deployment-scenarios)
    - [Deploy Oracle Sharding Containers](#deploy-oracle-sharding-containers)
      - [Deploy Oracle Sharding Containers on Docker](#deploy-oracle-sharding-containers-on-docker)
      - [Deploy Oracle Sharding Containers on Podman](#deploy-oracle-sharding-containers-on-podman)
    - [Deploy Oracle Database Sharding Containers using compose files](#deploy-oracle-database-sharding-containers-using-compose-files)
      - [Deploy Oracle Database Sharding Containers using docker compose](#deploy-oracle-database-sharding-containers-using-docker-compose)
      - [Deploy Oracle Database Sharding Containers using podman-compose](#deploy-oracle-database-sharding-containers-using-podman-compose)
  - [Support](#support)
  - [License](#license)
  - [Copyright](#copyright)
  
## Preparation Steps for running Oracle Sharding in Linux Containers
**Note :** All Steps or Commands in this guide is required to be run as `root` or `sudo` user.
* Please review following pre-requisites before you proceed to next sections:
  * Install Docker, if using Oracle Linux 7
    * Ensure to install [Docker Engine](https://docs.oracle.com/en/operating-systems/oracle-linux/docker/), if you are using Oracle Linux 7 system.
    * You need to install `docker-engine` and `docker-cli` using yum command.
    ```bash
    yum-config-manager --enable ol7_addons
    yum install docker-engine docker-cli
    yum start docker
    systemctl enable --now docker               
    ```                           
  * Install Podman, if using Oracle Linux 8
    * Ensure to install and configure [Podman release 4.6.1 or later](https://docs.oracle.com/en/learn/intro_podman/index.html#introduction) or later on Oracle Linux 8.9 or later to run Oracle Sharding on Podman.
    * You need to install `podman-docker` utility using dnf command.
      ```bash
      # Enable the Oracle Linux 8 AppStream repository
      dnf config-manager --enable ol8_appstream

      # Install Podman and Podman-docker
      dnf install -y podman podman-docker
      ```

## QuickStart
We recommend you start with Quickstart first to get familiar with Oracle Sharding in Linux Containers. Refer [QuickStart documentation](./docs/QUICKSTART.md)

Once you become familiar with Oracle Sharding in Linux Containers, you can explore more advanced setups, deployments, features, etc. as explained in detail in [Oracle Sharding in Containers Deployment Scenarios](#oracle-sharding-in-containers-deployment-scenarios)

## Building Oracle Sharding Container Images

To assist in building the images, you can use the [buildContainerImage.sh](containerfiles/buildContainerImage.sh) script.

**IMPORTANT:** Oracle Global Service Manager container is useful when you want to configure the Global Data Service Framework. The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases.

### Create Oracle Global Service Manager Image

**IMPORTANT:** You will have to provide the installation binaries of `Oracle Global Service Manager Oracle Database 23ai  (23.4) for Linux x86-64` and put them into the `containerfiles/<version>` folder. You only need to provide the binaries for the edition you are going to install. The binaries can be downloaded from the [Oracle Technology Network](http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). You also have to make sure to have internet connectivity for dnf package manager.
**Note:** Ensure not to uncompress the binaries.

The `buildContainerImage.sh` script is just a utility shell script that performs MD5 checks and is an easy way for beginners to get started. Expert users are welcome to directly call `podman build` with their preferred set of parameters. Before you build the image make sure that you have provided the installation binaries and put them into the right folder. Go into the **containerfiles** folder and run the **buildContainerImage.sh** the script as root or with sudo privileges:

```bash
./buildContainerImage.sh -v (Software Version)
./buildContainerImage.sh -v 23.4.0
```

For detailed usage of command, please execute the following command:

```bash
./buildContainerImage.sh -h
Usage: buildContainerImage.sh -v [version] -t [image_name:tag] [-e | -s] [-i] [-o] [container build option]
It builds a container image for a DNS server

Parameters:
   -v: version to build
   -i: ignores the MD5 checksums
   -t: user defined image name and tag (e.g., image_name:ta
   -o: passes on container build option (e.g., --build-arg SLIMMIMG=true for slim)

LICENSE UPL 1.0

Copyright (c) 2014,2024 Oracle and/or its affiliates.
```

### Create Oracle Database Image

To build Oracle Sharding on docker/container, you need to download and build Oracle 23.4.0 Database image,  refer [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md) of Oracle Single Database available on Oracle GitHub repository.

**Note**: You just need to create the image as per the instructions given in [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md) but you will create the container as per the steps given in this document under [Create Containers](#create-containers) section.

### Create Extended Oracle Database Image with Sharding Feature

After creating the base image using `buildContainerImage.sh` in the previous step, use `buildExtensions.sh` present under the extensions folder to build an extended image that will include the Sharding Feature. Please refer [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/extensions/README.md) of extensions folder of Oracle Single Database available on Oracle GitHub repository.

For example:

```bash
./buildExtensions.sh -a -x sharding -b oracle/database:23.4.0-ee  -t oracle/database-ext-sharding:23.4.0-ee -o "--build-arg BASE_IMAGE_VERSION=23.4.0"

Where:
"-x sharding"                                   is to specify to have sharding feature in the extended image
"-b oracle/database:23.4.0-ee"                  is to specify the Base image created in previous step
"oracle/database-ext-sharding:23.4.0-ee"        is to specify the name:tag for the extended image with Sharding Feature
-o "--build-arg BASE_IMAGE_VERSION=23.4.0"      is to specify the BASE_IMAGE_VERSION to clone from db-sharding git repo
```

Usage of `buildExtensions.sh` script-
```bash
./buildExtensions.sh -h

Usage: buildExtensions.sh -a -x [extensions] -b [base image] -t [image name] -v [version] [-o] [container build option]
Builds one of more Container Image Extensions.
  
Parameters:
   -a: Build all extensions
   -x: Space separated extensions to build. Defaults to all
       Choose from : k8s  patching  prebuiltdb  sharding  
   -b: Base image to use
   -v: Base version to extend (example 21.3.0)
   -t: name:tag for the extended image
   -o: passes on Container build option

LICENSE UPL 1.0

Copyright (c) 2024 Oracle and/or its affiliates. All rights reserved.
```

## Oracle Sharding in Containers Deployment Scenarios
### Deploy Oracle Database Sharding Containers
In case, you want to manually deploy the sharded database using Docker or Podman containers, refer to the below sections for the step by step procedure.

#### Deploy Oracle Sharding Containers on Docker

Refer to [Deploy Oracle Sharding Containers on Docker](./samples/container-files/docker-container-files/README.md) to deploy a  sharded database. This document provides the commands to deploy a Sharded Database using Docker with System Sharding or with User Defined Sharding.

**NOTE:** If you want to use Oracle 19c or Oracle 21c version based container images with Docker, you need to deploy on an Oracle Linux 7 host.

#### Deploy Oracle Sharding Containers on Podman

Refer to [Deploy Oracle Sharding Containers on Podman](./samples/container-files/podman-container-files/README.md) to deploy a  sharded database. This document provides the commands to deploy a Sharded Database using Podman with System Sharding or with User Defined Sharding.

**NOTE:** If you want to use Oracle 21c or Oracle 23ai version based container images with Podman, you need to deploy on an Oracle Linux 8 host.

### Deploy Oracle Database Sharding Containers using compose files

Please check the below sections to use compose files to deploy containers for the sharded database in an automated way.

#### Deploy Oracle Database Sharding Containers using docker compose

[Compose](https://docs.docker.com/compose/) is a tool for defining and running multi-container Docker applications. With "docker-compose", you use a YAML file to configure your application's services. Then, with a single command, you create and start all the services from your configuration. Please check [sample deployment using docker compose](./samples/compose-files/docker-compose.md) to deploy an Oracle Sharded Database on an Oracle Linux 7 Machine using Oracle 21c RDBMS and 21c GSM Docker Images.


#### Deploy Oracle Database Sharding Containers using podman-compose

For Oracle Linux 8 host machines,`podman-compose` can be used for deploying containers to create an Oracle Sharded database. You can use Oracle 23ai GSM and RDBMS Podman Images and can enable the SNR RAFT feature while deploying the Oracle Sharded Database. Please check [sample deployment using podman-compose](./samples/compose-files/podman-compose.md) to deploy an Oracle Sharded Database on an Oracle Linux 8 Machine using Oracle 23ai RDBMS and 23ai GSM Podman Images.

## Support

Oracle GSM and Sharding Database on Docker is supported on Oracle Linux 7. 
Oracle 23ai GSM and Sharding Database on Podman is supported on Oracle Linux 8 and onwards.


## License

To download and run Oracle GSM and Sharding Database, regardless whether inside or outside a Container, ensure to download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker and Podman images are, unless otherwise noted, released under UPL 1.0 license.


## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/