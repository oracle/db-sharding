# Replication in Oracle Sharding

Replication provides high availability, disaster recovery, and additional scalability for reads. A unit of replication can be a shard, a part of a shard, or a group of shards.

Replication topology in a sharded database is declaratively specified using GDSCTL command syntax. You can choose one of three technologies—Oracle Data Guard, Raft replication, or Oracle GoldenGate—to replicate your data. Oracle Sharding automatically deploys the specified replication topology to the procured systems, and enables data replication.

For details, refer [Oracle Sharding](https://docs.oracle.com/en/database/oracle/oracle-database/21/shard/index.html)

This README.md provides steps to deploy Oracle **Raft Replication** in containers. **Raft Replication* is supported from Oracle 23c. If you are planing to deploy **Shard-level Replication**, refer the page [Oracle Sharding in Linux Containers](../../README.md).

## Example of creating a Oracle Oracle Sharding Raft Replication in Linux Containers

Raft Replication is a built-in Oracle Sharding capability that integrates data replication with transaction execution in a sharded database. Raft replication enables fast automatic failover with zero data loss. If all shards are in the same data center, it is possible to achieve sub-second failover.

Raft replication is active/active; each shard can process reads and writes for a subset of data. This capability provides a uniform configuration with no primary or standby shards.

### Section 1 : Prerequisites for Oracle Oracle Sharding Raft Replication in Linux Containers

**IMPORTANT** : You must execute all the steps specified in this section (customized for your environment) before you proceed to the next section.

 * [How to build and run](../../README.md/how-to-build-and-run)
 * [Create Oracle Global Service Manager Image](../../README.md/create-oracle-global-service-manager-image)
 * [Create Oracle Database Image](../../README.md/create-oracle-database-image)
 * [Create Extended Oracle Database Image with Sharding Feature](../../README.md/create-extended-oracle-database-image-with-sharding-feature)
 * [Create Network Bridge](../../README.md/create-network-bridge)
 * [Setup Hostfile](../../README.md/setup-hostfile)
 * [Password Setup](../../README.md/password-setup)

### Section 2: Deploy Containers


#### Deploying Catalog Container

The shard catalog is a special-purpose Oracle Database that is a persistent store for SDB configuration data and plays a key role in the automated deployment and centralized management of a sharded database. It also hosts the gold schema of the application and the master copies of common reference data (duplicated tables)

##### Create Directory

You need to create mountpoint on the docker host to save datafiles for Oracle Sharding Catalog DB and expose as a volume to catalog container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/CATALOG directory and exposed as volume to catalog container.

```
mkdir -p /oradata/dbfiles/CATALOG
chown -R 54321:54321 /oradata/dbfiles/CATALOG
```

**Notes**:

* Change the ownership for data volume `/oradata/dbfiles/CATALOG` exposed to catalog container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
* If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instance Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

##### Create Container

Before performing catalog container, review the following notes carefully:

**Notes**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/CATALOG based on your enviornment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID enviornment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name catalog oracle/database:23.3.0-ee` to `--name catalog oracle/database:23.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, /oradata/dbfiles/CATALOG must contain the DB backup and it must not be in zipped format. E.g. /oradata/dbfiles/CATALOG/SEEDCDB where SEEDCDB is the cold backup and contains datafiles and PDB.



#### Deploying Shard Containers

A database shard is a horizontal partition of data in a database or search engine. Each individual partition is referred to as a shard or database shard. You need to create mountpoint on docker host to save datafiles for Oracle Sharding DB and expose as a volume to shard container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/ORCL1CDB directory and exposed as volume to shard container.

##### Create Directories

```
mkdir -p /oradata/dbfiles/ORCL1CDB
mkdir -p /oradata/dbfiles/ORCL2CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL2CDB
chown -R 54321:54321 /oradata/dbfiles/ORCL1CDB
```

**Notes**:

* Change the ownership for data volume `/oradata/dbfiles/ORCL1CDB` and `/oradata/dbfiles/ORCL2CDB` exposed to shard container as it has to be writable by oracle "oracle" (uid: 54321) user inside the container.
* If this is not changed then database creation will fail. For details, please refer, [oracle/docker-images for Single Instace Database](https://github.com/oracle/docker-images/tree/master/OracleDatabase/SingleInstance).

##### Shard1 Container

Before creating shard1 container, review the following notes carefully:

**Notes**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/ORCL1CDB based on your environment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID environment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard1 oracle/database:23.3.0-ee` to `--name shard1 oracle/database:23.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, `/oradata/dbfiles/ORCL1CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL1CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.



##### Shard2 Container

Before creating shard1 container, review the following notes carefully:

**Notes**

* Change environment variable such as ORACLE_SID, ORACLE_PDB based on your env.
* Change /oradata/dbfiles/ORCL2CDB based on your environment.
* By default, sharding setup creates new database under `/opt/oracle/oradata` based on ORACLE_SID environment variable.
* If you are planing to perform seed cloning to expedite the sharding setup using existing cold DB backup, you need to replace following `--name shard2 oracle/database:23.3.0-ee` to `--name shard2 oracle/database:23.3.0-ee /opt/oracle/scripts/setup/runOraShardSetup.sh`
  * In this case, `/oradata/dbfiles/ORCL2CDB` must contain the DB backup and it must not be zipped. E.g. `/oradata/dbfiles/ORCL2CDB/SEEDCDB` where `SEEDCDB` is the cold backup and contains datafiles and PDB.


#### Deploying GSM Container

The Global Data Services framework consists of at least one global service manager, a Global Data Services catalog, and the GDS configuration databases. You need to create mountpoint on docker host to save gsm setup related file for Oracle Global Service Manager and expose as a volume to GSM container. This volume can be local on a docker host or exposed from your central storage. It contains a file system such as EXT4. During the setup of this README.md, we used /oradata/dbfiles/GSMDATA directory and exposed as volume to GSM container.

##### Create Directory

```
mkdir -p /oradata/dbfiles/GSMDATA
chown -R 54321:54321 /oradata/dbfiles/GSMDATA


#### Create GSM Standby Container

You need GSM standby container to serve the connection when master GSM fails.

##### Create Directory

```
mkdir -p /oradata/dbfiles/GSM2DATA
chown -R 54321:54321 /oradata/dbfiles/GSM2DATA
```

##### Create Container



Section 3: Test/Validation Oracle Sharding Raft Replication

Section 4: Copyright
Copyright (c) 2014-2012 Oracle and/or its affiliates. All rights reserved.
