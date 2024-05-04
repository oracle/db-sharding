# Oracle Sharding in Linux Containers

Learn about container deployment options for Oracle Sharding in Linux Containers Release 23ai (v23.4)

## Overview of Oracle Sharding in Linux Containers

Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle Databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

This project offers sample container files to facilitate installation, configuration, and environment setup for DevOps users. For more information about Oracle Database, see: [Oracle Sharded Database Management Documentation](http://docs.oracle.com/en/database/).

Review each of the sections of this README in the order given. After reviewing each section of the README, you can skip the image or container creation sections that do not apply to you.

This project offers example container files for the following: 

* Oracle Database 23ai Global Service Manager (GSM/GDS) (23.4.0) for Linux x86-64
* Older Releases: Oracle 19c (19.3) and Oracle 21c (21.3) for Linux x86-64


## Using this Documentation
To create an Oracle Sharding Container environment, follow these steps:

- [Oracle Sharding in Linux Containers](#oracle-sharding-in-linux-containers)
  - [Overview of Oracle Sharding in Linux Containers](#overview-of-oracle-sharding-in-linux-containers)
  - [Using this Documentation](#using-this-documentation)
  - [Preparation Steps for running Oracle Sharding in Linux Containers](#preparation-steps-for-running-oracle-sharding-in-linux-containers)
  - [QuickStart](#quickstart)
  - [Building Oracle Sharding Container Images](#building-oracle-sharding-container-images)
    - [Building Oracle Global Service Manager Image](#building-oracle-global-service-manager-image)
    - [Building Oracle Database Image](#building-oracle-database-image)
    - [Building Extended Oracle Database Image with Sharding Feature](#building-extended-oracle-database-image-with-sharding-feature)
  - [Oracle Sharding in Containers Deployment Scenarios](#oracle-sharding-in-containers-deployment-scenarios)
    - [Deploy Oracle Database Sharding Containers](#deploy-oracle-database-sharding-containers)
      - [Deploy Oracle Sharding Containers on Podman](#deploy-oracle-sharding-containers-on-podman)    
      - [Deploy Oracle Sharding Containers on Docker](#deploy-oracle-sharding-containers-on-docker)
  - [Support](#support)
  - [License](#license)
  - [Copyright](#copyright)
  
## Preparation Steps for running Oracle Sharding in Linux Containers
**Note :** All Steps or Commands in this guide must be run as `root` or with a `sudo` user.
* Before you proceed, complete the following prerequisites for your platform:
  * If you are using Oracle Linux 7, then install Docker.
    * If you are using an Oracle Linux 7 system, then you must install [Docker Engine](https://docs.oracle.com/en/operating-systems/oracle-linux/docker/).
    * Install `docker-engine` and `docker-cli` using yum command.
    ```bash
    yum-config-manager --enable ol7_addons
    yum install docker-engine docker-cli
    yum start docker
    systemctl enable --now docker               
    ```                           
  * If you are using Oracle Linux 8, then Install Podman.
    * You must install and configure [Podman release 4.6.1 or later](https://docs.oracle.com/en/learn/intro_podman/index.html#introduction) or later on Oracle Linux 8.9 or later to run Oracle Sharding on Podman.
    * You need to install `podman-docker` utility using dnf command.
      ```bash
      # Enable the Oracle Linux 8 AppStream repository
      dnf config-manager --enable ol8_appstream

      # Install Podman and Podman-docker
      dnf install -y podman podman-docker
      ```
    * If SELinux is enabled on podman host, then install the following package as well:
      ```bash
      dnf install -y selinux-policy-devel
      ```

## QuickStart
Oracle recommends that you start with the Quickstart to become familiar with Oracle Sharding in Linux Containers. See: [QuickStart documentation](./docs/QUICKSTART.md).

After you become familiar with Oracle Sharding in Linux Containers, you can explore more advanced setups, deployments, features, and so on, as explained in detail in [Oracle Sharding in Containers Deployment Scenarios](#oracle-sharding-in-containers-deployment-scenarios).

**Note:**
* Ensure that you have enough space in `/var/lib/containers` while building the Oracle Sharding images. Also, if required use `export TMPDIR=</path/to/tmpdir>` for Podman to refer to any other folder as the temporary podman cache location instead of the default `/tmp` location.

## Building Oracle Sharding Container Images

To assist with building the images, you can use the [buildContainerImage.sh](containerfiles/buildContainerImage.sh) script.

**IMPORTANT:** Oracle Global Service Manager (GDS) container is useful when you want to configure the Global Data Service Framework. A Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases.

### Building Oracle Global Service Manager Image

**IMPORTANT:** To create an Oracle Global Service Manager image (GSM image), you must provide the installation binaries of `Oracle Global Service Manager Oracle Database 23ai (23.4) for Linux x86-64` and put them into the `containerfiles/<version>` folder. You only need to provide the binaries for the edition you are going to install. The binaries can be downloaded from the [Oracle Technology Network](http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html). You must ensure that you have internet connectivity for the DNF package manager.

**Note:** Do not uncompress the binaries.

The `buildContainerImage.sh` script is just a utility shell script that performs MD5 checks. This script provides an easy way for beginners to get started. Expert users can directly call `podman build` with their preferred set of parameters. Before you build the image, ensure that you have provided the installation binaries and put them into the right folder. Go into the **containerfiles** folder and run the **buildContainerImage.sh**  script as `root` or with `sudo` privileges:

```bash
./buildContainerImage.sh -v (Software Version)
./buildContainerImage.sh -v 23.4.0
```

For detailed usage information for `buildContainerImage.sh`, run the following command:

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

### Building Oracle Database Image

To build Oracle Sharding on Docker and a container, download and build an Oracle 23.4.0 Database Image. See the Oracle Database Single Instance [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md), which is available on the Oracle GitHub repository.

**Note**: Use the [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md) to create the image, and do not use the container instructions. For the container, use the steps given in this document under the [Oracle Sharding in Containers Deployment Scenarios](#oracle-sharding-in-containers-deployment-scenarios) section.

### Building Extended Oracle Database Image with Sharding Feature

After creating the base image using `buildContainerImage.sh` in the previous step, use the `buildExtensions.sh` script that is under the `extensions` folder to build an extended image. This extended image will include the Sharding Feature. For more information, the [README.MD](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/extensions/README.md) in the `extensions` folder for the Oracle Single Instance Database, which is available on the Oracle GitHub repository.

For example:

```bash
./buildExtensions.sh -a -x sharding -b oracle/database:23.4.0-ee  -t oracle/database-ext-sharding:23.4.0-ee -o "--build-arg BASE_IMAGE_VERSION=23.4.0"

Where:
"-x sharding"                                   is to specify to have sharding feature in the extended image
"-b oracle/database:23.4.0-ee"                  is to specify the Base image created in previous step
"oracle/database-ext-sharding:23.4.0-ee"        is to specify the name:tag for the extended image with Sharding Feature
-o "--build-arg BASE_IMAGE_VERSION=23.4.0"      is to specify the BASE_IMAGE_VERSION to clone from db-sharding git repo
```

To see more usage instructions for the `buildExtensions.sh` script, run the following command: 
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
If you want to manually deploy the sharded database using Docker or Podman containers, then use the sections that follow to see the step by step procedure.

#### Deploy Oracle Sharding Containers on Podman

To deploy a sharded database on Podman, see: [Deploy Oracle Sharding Containers on Podman](./samples/container-files/podman-container-files/README.md). This document provides the commands that you need to deploy a Sharded Database using Podman with System Sharding or with System Sharding with SNR RAFT enabled or with User Defined Sharding.

**NOTE:** If you want to use Oracle Database 21c or Oracle Database 23ai release-based container images with Podman, then you must deploy on an Oracle Linux 8 host.

#### Deploy Oracle Sharding Containers on Docker

To deploy a sharded database on Docker, see: [Deploy Oracle Sharding Containers on Docker](./samples/container-files/docker-container-files/README.md). This document provides the commands that you need to deploy a Sharded Database using Docker with either System Sharding or with User Defined Sharding.

**NOTE:** If you want to use the Oracle Database 19c or Oracle Database 21c release-based container images with Docker, then you must deploy on an Oracle Linux 7 host.

## Support

Oracle Global Service Manager (GSM) and Sharding Database on Docker is supported on Oracle Linux 7. 
Oracle Database 23ai GSM and Sharding Database on Podman is supported on Oracle Linux 8 and onwards.


## License

To download and run Oracle Global Service Manager (GSM) and Sharding Database, either inside or outside a Container, you must download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker and Podman images are, unless otherwise noted, released under UPL 1.0 license.


## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/
