# Oracle Sharding Tools Library

[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/sdb-terraform-oci
[SDB-Mid-Tier-Routing]: https://github.com/oracle/db-sharding/tree/master/Mid-Tier-Routing
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/
[OCI]: https://www.oracle.com/cloud/ 

- [About](#about)
- [Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure]
(#terraform-based-deployment-of-oracle-sharded-database-on-oracle-cloud-infrastructure)
  * [Overview](#overview)
  * [Details](#details)
- [Terraform based On-Premise deployment of Oracle sharded database](#terraform-based-deployment-of-oracle-sharded-database-on-premise)
  * [Overview](#overview-2)
  * [Details](#details-2)
- [Mid-tier routing for use in Oracle sharded database client applications](#mid-tier-routing-for-use-in-oracle-sharded-database-client-applications)
  * [Overview](#overview-3)
  * [Details](#details-3)
- [Routing implementations for use in Oracle sharding client applications](#routing-implementations-for-use-in-oracle-sharding-client-applications)
  * [Motivation](#motivation)
  * [Overview](#overview-4)
    + [Routing Tables](#routing-tables)
    + [Sharding Metadata](#sharding-metadata)
    + [Metadata Queries](#metadata-queries)
    + [Splitter](#splitter)
  * [Setup](#setup)
  * [Examples](#examples)
    + [Schema](#schema)

## About

This library provides Oracle Sharding tools for the following :

* Terraform based On-Premise deployment of Oracle sharded database.
* Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure.
* Mid-tier routing for use in Oracle sharded database client applications.
* Routing implementations for use in Oracle sharding client applications.

If you want to learn more about Oracle sharding a.k.a Oracle Sharded Database, please refer to the [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Oracle Sharding and Oracle Sharded Database (SDB) are interchangeably used names in the documentation and refer to the same entity. 

 
## Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure

### Overview 

Provides Terraform scripts that provisions the necessary components and resources for a quick and easy setup of [Oracle Sharded Database][SDB] (SDB) on [Oracle Cloud Infrastructure][OCI] (OCI). It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform-OCI][SDB-terraform-oci] documentation.

## Terraform based deployment of Oracle sharded database On-Premise

### Overview 

Provides Oracle Sharded Database (SDB) Terraform modules and scripts that provisions the necessary components and resources required for a quick and easy setup of Oracle Sharded Database on-prem. It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform][SDB-terraform-onprem] documentation.

## Mid-tier routing for use in Oracle sharded database client applications

### Overview

Please refer to the Sharded database [Mid-Tier Routing introduction][MTR-Intro] for an overview.

### Details

For more details, please refer to the [SDB Mid-Tier routing][SDB-Mid-Tier-Routing] documentation. 


## Routing implementations for use in Oracle sharding client applications 

NOTE: this is a *beta* 0.1 version of the library.

### Motivation

When using Oracle Sharding ...

The solution is to use the same routing algorithms, which are used by the database.
Oracle Universal Connection Pool, for instance, does that. It caches the routing information and 
then applies the same functions as the database does to the sharding key.

Routing information is available from the set of public views:
 * `LOCAL_CHUNK_TYPES` view provides the information about the sharding type,
   as well as the name of the root sharded table,
 * `LOCAL_CHUNK_COLUMNS` view provides the sharding key columns and their types,
 * `LOCAL_CHUNKS` view provides the information about chunks available and 
   the ranges they store.

On the catalog database, `LOCAL_CHUNKS` view provides the information about all the chunks and

We propose a lightweight Java library for reading and manipulating that information.
It can be used to develop streaming applications which would take into
account Oracle Sharded Database data distribution.

### Overview

The library consist of several parts:
 * Generic routing table interfaces
 * Sharding metadata implementation
 * Metadata reader (encapsulated select queries)
 * Tools for efficient parallel splitting with respect to sharding key

#### Routing Tables

`RoutingTable` interface is a generic multimap (may contain multiple objects for the same key) 
from routing key to any object. The main methods it has are `lookup`, which returns all
the objects for a given key and `find`, which returns iterator (actually 
'iterator factory' in java terms), for lazily traversing objects satisfying the given key.    

By design any specific implementation should be thread-safe.
 
It is not a java collection since it's not really mutable as Collection interface 
requires it to be. Instead all modifications are made through a special object 
which allows to make them atomically. 

Please see CustomDataSorting example, where it is used to map routing keys to files directly.

`RoutingKey` is only there to prevent passing invalid object as a routing key, 
see javadoc on that class.

#### Sharding Metadata

`OracleShardingMetadata` is mainly a routing key factory, but it can also store 
information on the chunk topology. Please see Oracle Sharding documentation on the concepts.
It can be used only as routing key builder, or it can be used to create a 
special type of routing table which maps RoutingKeys to Chunks: `ChunkTable`. 
See `GenToFile` example for a basic example of it's usage. 

All the `ChunkTable` instances created by `OracleShardingMetadata.createChunkTable()` method,
will be updated when the `OracleShardingMetadata.updateChunkTables()` 
method is called.

The instance of this class can be created by `MetadataReader` or manually,
providing with required type information if the application is aware of it.

#### Metadata Queries

`MetadataReader` class encapsulates queries to the catalog database to read the 
information on sharding. The full information is represented by 
`ShardConfiguration` class, which can be serialized to cache this information
or pass it to some other server (examples TBD).

#### Splitter

`PartitionEngine` allows to split objects with respect to the routing 
table in parallel. See `DirectLoad` and `ParallelLoad` examples for more information.

### Setup

The library requires OJDBC 12.2 library, which can be downloaded from the Oracle website. 
If you are using Maven, it makes sense to install it into your local repository: 

```
mvn install:install-file -Dfile={ojdbc-jar-file}.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
```

Or in case you have OJDBC pom file:

```
mvn install:install-file -Dfile={ojdbc-jar-file}.jar -DpomFile={ojdbc-pom}.xml
```

Or in case you have the maven oracle repository set up, it will be resolved automatically.

In order to build, test and install repositories, just make `mvn install`:

```
cd sharding
mvn install
```

### Examples

Examples require a sharding environment with a shard user created:

```
    alter session enable shard ddl;
    
    create user test identified by 123;
    grant connect, resource to test;
    grant unlimited tablespace to test;
    grant create tablespace to test;
    grant gsmadmin_role to test;
```

Note, that the user must have `gsmadmin_role` in order to read
`LOCAL_CHUNKS` view from the catalog database correctly.

#### Schema

Most of the examples are working with a single sharded table:

```
    create tablespace set deflts datafile template autoextend on;

    create sharded table log(
      cust_id varchar2(128),
      ip_addr varchar2(128),
      hits integer,
      primary key(cust_id, ip_addr))
      partition by consistent hash (cust_id) 
        tablespace set deflts;
```

You can also create this table (along with the tablespace set) by running 
`oracle.sharding.examples.schema.CreateSchema` class from the examples.
