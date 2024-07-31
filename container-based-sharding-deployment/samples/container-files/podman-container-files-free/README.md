# Oracle Globally Distributed Database Containers using Oracle Database FREE Images on Podman

In this installation guide, we deploy Oracle Globally Distributed Database Containers using Oracle Database FREE Images on Podman. This page provides detailed steps for various scenarios of Oracle Globally Distributed Database deployments using Oracle Database FREE Images using Podman Containers.
- [Oracle Globally Distributed Database Containers using Oracle Database FREE Images on Podman](#oracle-globally-distributed-database-containers-using-oracle-database-free-images-on-podman)
  - [Prerequisites](#prerequisites)
  - [Network Management](#network-management)
    - [Macvlan Network](#macvlan-network)
    - [Ipvlan Network](#ipvlan-network)
    - [Bridge Network](#bridge-network)
  - [Setup Hostfile](#setup-hostfile)
  - [Password Management](#password-management)
  - [SELinux Configuration on Podman Host](#selinux-configuration-on-podman-host)
  - [Deploy Oracle Globally Distributed Database Containers using Oracle Database FREE Images](#deploy-oracle-globally-distributed-database-containers)
    - [Deploy Oracle Globally Distributed Database with System-Managed Sharding using Oracle Database FREE Images](#deploy-oracle-globally-distributed-database-with-system-managed-sharding-using-oracle-database-free-images)
    - [Deploy Oracle Globally Distributed Database with System-Managed Sharding with RAFT Replication enabled using Oracle Database FREE Images](#deploy-oracle-globally-distributed-database-with-system-managed-sharding-with-raft-replication-enabled-using-oracle-database-free-images)
    - [Deploy Oracle Globally Distributed Database with User-Defined Sharding using Oracle Database FREE Images](#deploy-oracle-globally-distributed-database-with-user-defined-sharding-using-oracle-database-free-images)
- [Support](#support)
- [License](#license)
- [Copyright](#copyright)



## Prerequisites

This section provides the prerequisite steps to be completed before deploying an Oracle Globally Distributed Database using Podman Containers using Oracle Database FREE Images. In involves the podman network creation, creation of encrypted file with secrets etc. 


### Network Management

Before creating a container, create the podman network by creating podman network bridge based on your environment. If you are using the bridge name with the network subnet mentioned in this README.md then you can use the same IPs mentioned in [Deploy Oracle Globally Distributed Database Containers using Oracle Database FREE Images](#create-containers) section.

#### Macvlan Network

To create a podman network with `macvlan` driver:

```bash
podman network create -d macvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=ens5 shard_pub1_nw
```

#### Ipvlan Network

To create a podman network with `ipvlan` driver:

```bash
podman network create -d ipvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=ens5 shard_pub1_nw
```

If you are planning to create a test env within a single machine, you can use a podman bridge but these IPs will not be reachable on the user network.

#### Bridge Network

To create a podman network with `bridge` driver:

```bash
podman network create --driver=bridge --subnet=10.0.20.0/24 shard_pub1_nw
```

**Note:** You can change subnet and choose one of the above mentioned podman network bridge based on your environment.

### Setup Hostfile

**Note:** You can skip this step of creating a Hostfile when you are using a DNS for the IP resolution.

All containers will share a host file for name resolution.  The shared hostfile must be available to all containers. Create the empty shared host file (if it doesn't exist) at `/opt/containers/shard_host_file`:

For example:

```bash
mkdir /opt/containers
rm -rf /opt/containers/shard_host_file && touch /opt/containers/shard_host_file
```

Add the following host entries in `/opt/containers/shard_host_file` as Oracle Database Containers do not have root access to modify the /etc/hosts file. This file must be pre-populated. You can change these entries based on your environment and network setup.

```text
127.0.0.1       localhost.localdomain           localhost
10.0.20.100     oshard-gsm1.example.com         oshard-gsm1
10.0.20.101     oshard-gsm2.example.com         oshard-gsm2
10.0.20.102     oshard-catalog-0.example.com    oshard-catalog-0
10.0.20.103     oshard1-0.example.com           oshard1-0
10.0.20.104     oshard2-0.example.com           oshard2-0
10.0.20.105     oshard3-0.example.com           oshard3-0
10.0.20.106     oshard4-0.example.com           oshard4-0
```

### Password Management

* Specify the secret volume for resetting database users password during catalog and shard setup. It can be a shared volume among all the containers

  ```bash
  mkdir /opt/.secrets/
  cd /opt/.secrets
  openssl genrsa -out key.pem
  openssl rsa -in key.pem -out key.pub -pubout
  ```

* Edit the `/opt/.secrets/pwdfile.txt` and seed the password. It will be a common password for all the database users. Execute following command:

  ```bash
  vi /opt/.secrets/pwdfile.txt
  ```
  **Note**: Enter your secure password in the above file and save the file.

* After seeding password and saving the `/opt/.secrets/pwdfile.txt` file, execute following command:
  ```bash
  openssl pkeyutl -in /opt/.secrets/pwdfile.txt -out /opt/.secrets/pwdfile.enc -pubin -inkey /opt/.secrets/key.pub -encrypt
  rm -rf /opt/.secrets/pwdfile.txt
  ```
  We recommend using Podman secrets to be used inside the containers. Execute the following command to create podman secrets:
  
  ```bash
  podman secret create pwdsecret /opt/.secrets/pwdfile.enc
  podman secret create keysecret /opt/.secrets/key.pem

  podman secret ls
  ID                         NAME        DRIVER      CREATED        UPDATED
  547eed65c01d525bc2b4cebd9  keysecret   file        8 seconds ago  8 seconds ago
  8ad6e8e519c26e9234dbcf60a  pwdsecret   file        8 seconds ago  8 seconds ago
  ```

**Note:** This password and key secrets are being used for initial Oracle Globally Distributed Database topology setup. Once the Oracle Globally Distributed Database topology setup is completed, user must change the topology passwords based on his enviornment.

## SELinux Configuration on Podman Host
To run Podman containers in an environment with SELinux enabled, you must configure an SELinux policy for the containers. To check if your SELinux is enabled or not, run the `getenforce` command.
With Security-Enhanced Linux (SELinux), you must set a policy to implement permissions for your containers. If you do not configure a policy module for your containers, then they can end up restarting indefinitely or other permission errors. You must add all Podman host nodes for your cluster to the policy module `shard-podman`, by installing the necessary packages and creating a type enforcement file (designated by the .te suffix) to build the policy, and load it into the system. 

In the following example, the Podman host `podman-host` is configured in the SELinux policy module `shard-podman`: 

Copy [shard-podman.te](../../../containerfiles/shard-podman.te) to `/var/opt` folder in your host and then execute below-
```bash
cd /var/opt
make -f /usr/share/selinux/devel/Makefile shard-podman.pp
semodule -i shard-podman.pp
semodule -l | grep shard-pod
```
## Deploy Oracle Globally Distributed Database Containers using Oracle Database FREE Images

Refer to the relevant section depending on whether you want to deploy the Oracle Globally Distributed Database using System-Managed Sharding, System-Managed Sharding with RAFT Replication enabled or User-Defined Sharding.

### Deploy Oracle Globally Distributed Database with System-Managed Sharding using Oracle Database FREE Images

Refer to [Sample Oracle Globally Distributed Database with System-Managed Sharding deployed manually using Podman Containers and Oracle Database FREE Images](./podman-sharded-database-free-with-system-sharding.md) to deploy a sample Oracle Globally Distributed Database with System-Managed sharding using podman containers and Oracle Database FREE Images.


### Deploy Oracle Globally Distributed Database with System-Managed Sharding with RAFT Replication Enabled using Oracle Database FREE Images

Refer to [Sample Oracle Globally Distributed Database with System-Managed Sharding with RAFT Replication enabled deployed manually using Podman Containers and Oracle Database FREE Images](./podman-sharded-database-free-with-system-sharding-with-snr-raft-enabled.md) to deploy a sample Oracle Globally Distributed Database with System-Managed sharding with RAFT Replication enabled using podman containers and Oracle Database FREE Images.

**NOTE:** RAFT Replication Feature is available only for Oracle 23ai RDBMS and Oracle 23ai GSM version.

### Deploy Oracle Globally Distributed Database with User-Defined Sharding using Oracle Database FREE Images

Refer to [Sample Oracle Globally Distributed Database with User-Defined Sharding deployed manually using Podman Containers and Oracle Database FREE Images](./podman-sharded-database-free-with-user-defined-sharding.md) to deploy a sample Oracle Globally Distributed Database with User-Defined sharding using Podman containers and Oracle Database FREE Images.


## Support

Oracle Globally Distributed Database on Docker is supported on Oracle Linux 7. 
Oracle Globally Distributed Database on Podman is supported on Oracle Linux 8 and onwards.


## License

To run Oracle Globally Distributed Database, regardless whether inside or outside a Container, ensure to download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker and Podman images are, unless otherwise noted, released under UPL 1.0 license.


## Copyright

Copyright (c) 2022 - 2024 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/