## Routing implementations for use in Oracle Globally Distributed Database client applications 

NOTE: this is a *beta* 0.1 version of the library.

### Motivation

When using Oracle Globally Distributed Database ...

The solution is to use the same routing algorithms that are used by the database.
For example, Oracle Universal Connection Pool caches the routing information and 
then applies the same functions as the database does to the sharding key.

Routing information is available from the set of public views:
 * `LOCAL_CHUNK_TYPES` view provides the information about the sharding type,
   as well as the name of the root sharded table,
 * `LOCAL_CHUNK_COLUMNS` view provides the sharding key columns and their types,
 * `LOCAL_CHUNKS` view provides the information about chunks available and 
   the ranges they store.

On the catalog database, the view `LOCAL_CHUNKS` provides the information about all the chunks. 

In this example, we propose a lightweight Java library for reading and manipulating that information.
It can be used to develop streaming applications that take into
account Oracle Globally Distributed Database data distribution.

### Overview

The library consist of several parts:
 * Generic routing table interfaces
 * Sharding metadata implementation
 * Metadata reader (encapsulated select queries)
 * Tools for efficient parallel splitting with respect to sharding key

#### Routing Tables

The `RoutingTable` interface is a generic multimap (may contain multiple objects for the same key) 
from routing key to any object. The main methods it has are `lookup`, which returns all
the objects for a given key and `find`, which returns iterator (actually 
'iterator factory' in java terms), for lazily traversing objects satisfying the given key.    

By design, any specific implementation should be thread-safe.
 
It is not a Java collection because it's not really mutable as a Collection interface 
requires it to be. Instead, all modifications are made through a special object, 
which allows the modifications to be made atomically. 

See the CustomDataSorting example, where it is used to map routing keys to files directly.

`RoutingKey` is only there to prevent passing invalid object as a routing key, 
see the Java documentation on that class.

#### Sharding Metadata

`OracleShardingMetadata` is mainly a routing key factory, but it can also store 
information on the chunk topology. See Oracle Sharding documentation to learn more about the concepts.
This metadata can be used only as routing key builder, or it can be used to create a 
special type of routing table that maps RoutingKeys to Chunks: `ChunkTable`. 
See `GenToFile` example for a basic example of its usage. 

All the `ChunkTable` instances created by `OracleShardingMetadata.createChunkTable()` method,
will be updated when the `OracleShardingMetadata.updateChunkTables()` 
method is called.

The instance of this class can be created by `MetadataReader`, or manually,
providing with required type information if the application is aware of it.

#### Metadata Queries

`MetadataReader` class encapsulates queries to the catalog database to read the 
information on sharding. The full information is represented by 
`ShardConfiguration` class, which can be serialized to cache this information
or pass it to some other server.

#### Splitter

`PartitionEngine` enables objects to be split with respect to the routing 
table in parallel. See `DirectLoad` and `ParallelLoad` examples for more information.

### Setup

The library requires the OJDBC 12.2 library, which can be downloaded from the Oracle website. 
If you are using Maven, then it makes sense to install it into your local repository: 

```
mvn install:install-file -Dfile={ojdbc-jar-file}.jar -DgroupId=com.oracle.jdbc -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar
```

If you are using OJDBC pom file:

```
mvn install:install-file -Dfile={ojdbc-jar-file}.jar -DpomFile={ojdbc-pom}.xml
```

If you have the Maven Oracle repository set up, then it will be resolved automatically.

To build, test and install repositories, run `mvn install`:

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

Note that the user running the commands must have been granted `gsmadmin_role` to read the 
`LOCAL_CHUNKS` view from the catalog database correctly.

#### Schema

Most of the examples work with a single sharded table:

```
    create tablespace set deflts in shardspace shardspaceora using template (datafile size 16m autoextend on);

    create sharded table log(
      cust_id varchar2(128),
      ip_addr varchar2(128),
      hits integer)
      partition by consistent hash (cust_id) 
        tablespace set deflts;
```

You can also create this table (along with the tablespace set) by running 
`oracle.sharding.examples.schema.CreateSchema` class from the examples.