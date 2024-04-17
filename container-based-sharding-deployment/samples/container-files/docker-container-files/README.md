# Oracle Sharding Containers on Docker

Docker is used on Oracle Linux 7 Host Machines to create containers. This page provides the details to manually create the docker containers to deploy a Sharded Database.

- [Prerequisites](#prerequisites)
  - [Create Network Bridge](#create-network-bridge)
    - [Macvlan Bridge](#macvlan-bridge)
    - [Ipvlan Bridge](#ipvlan-bridge)
    - [Bridge](#bridge)
  - [Setup Hostfile](#setup-hostfile)
  - [Password Setup](#password-setup)
  - [Create Containers](#create-containers)
    - [Deploy Sharded Database with System Sharding](#deploy-sharded-database-with-system-sharding)
    - [Deploy Sharded Database with User Defined Sharding](#deploy-sharded-database-with-user-defined-sharding)
- [Support](#support)
- [License](#license)
- [Copyright](#copyright)



## Prerequisites

This section provides the prerequisite steps to be completed before deploying an Oracle Sharded Database using Docker Containers. In involves the docker network creation, creation of encrypted file with secrets etc. 


### Create Network Bridge

Before creating a container, create the docker network by creating docker network bridge based on your environment. If you are using the bridge name with the network subnet mentioned in this README.md then you can use the same IPs mentioned in [Create Containers](#create-containers) section.

#### Macvlan Bridge

```bash
docker network create -d macvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=ens5 shard_pub1_nw
```

#### Ipvlan Bridge

```bash
docker network create -d ipvlan --subnet=10.0.20.0/24 --gateway=10.0.20.1 -o parent=ens5 shard_pub1_nw
```

If you are planning to create a test env within a single machine, you can use a docker bridge but these IPs will not be reachable on the user network.

#### Bridge

```bash
docker network create --driver=bridge --subnet=10.0.20.0/24 shard_pub1_nw
```

**Note:** You can change subnet and choose one of the above mentioned docker network bridge based on your environment.

### Setup Hostfile

All containers will share a host file for name resolution.  The shared hostfile must be available to all containers. Create the shared host file (if it doesn't exist) at `/opt/containers/shard_host_file`:

For example:

```bash
mkdir /opt/containers
rm -rf /opt/containers/shard_host_file && touch /opt/containers/shard_host_file
```

Add the following host entries in `/opt/containers/shard_host_file` as Oracle database containers do not have root access to modify the `/etc/hosts` file. This file must be pre-populated. You can change these entries based on your environment and network setup.

```bash
127.0.0.1       localhost.localdomain           localhost
10.0.20.100     oshard-gsm1.example.com         oshard-gsm1
10.0.20.101     oshard-gsm2.example.com         oshard-gsm2
10.0.20.102     oshard-catalog-0.example.com    oshard-catalog-0
10.0.20.103     oshard1-0.example.com           oshard1-0
10.0.20.104     oshard2-0.example.com           oshard2-0
10.0.20.105     oshard3-0.example.com           oshard3-0
10.0.20.106     oshard4-0.example.com           oshard4-0
```

### Password Setup

Specify the secret volume for resetting database users password during catalog and shard setup. It can be a shared volume among all the containers

```bash
mkdir /opt/.secrets/
openssl genrsa -out /opt/.secrets/key.pem
openssl rsa -in /opt/.secrets/key.pem -out /opt/.secrets/key.pub -pubout
```

Edit the `/opt/.secrets/pwdfile.txt` and seed the password. It will be a common password for all the database users. Execute following command:

```bash
vi /opt/.secrets/pwdfile.txt
```

**Note**: Enter your secure password in the above file and save the file.

After seeding password and saving the `/opt/.secrets/pwdfile.txt` file, then execute the following command:
```bash
openssl pkeyutl -in /opt/.secrets/pwdfile.txt -out /opt/.secrets/pwdfile.enc -pubin -inkey /opt/.secrets/key.pub -encrypt
rm -f /opt/.secrets/pwdfile.txt
chown 54321:54321 /opt/.secrets/pwdfile.enc
chown 54321:54321 /opt/.secrets/key.pem
chown 54321:54321 /opt/.secrets/key.pub
chmod 400 /opt/.secrets/pwdfile.enc
chmod 400 /opt/.secrets/key.pem
chmod 400 /opt/.secrets/key.pub
```

This password key is being used for initial sharding topology setup. Once the sharding topology setup is completed, user must change the sharding topology passwords based on his enviornment.

## Create Containers

Refer to the relevant section depending on whether you want to deploy the Sharded Database using System Sharding or User Defined Sharding.

### Deploy Sharded Database with System Sharding

Refer to [Sample Sharded Database with System Sharding deployed manually using Docker Containers](./docker-sharded-database-with-system-sharding.md) to deploy a sample sharded database with system sharding using docker containers.

### Deploy Sharded Database with User Defined Sharding

Refer to [Sample Sharded Database with User Defined Sharding deployed manually using Docker Containers](./docker-sharded-database-with-user-defined-sharding.md) to deploy a sample sharded database with User Defined sharding using docker containers.


## Support

Oracle 19c or Oracle 21c GSM and RDBMS is supported for Oracle Linux 7.

## License

To download and run Oracle GSM and Sharding Database, regardless whether inside or outside a Docker container, you must download the binaries from the Oracle website and accept the license indicated at that page.

All scripts and files hosted in this project and GitHub docker-images/OracleDatabase repository required to build the Docker images are, unless otherwise noted, released under UPL 1.0 license.

## Copyright

Copyright (c) 2022, 2023 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/