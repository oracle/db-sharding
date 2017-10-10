# Streaming and Loading Sata with Oracle Sharding

## Motivation

When using Oracle Sharding ...

The solution is to use the same routing algorithms, which are used by the database.
Oracle Universal Connection Pool, for instance, does that. It caches the routing information and 
then applies the same functions as the database does to the sharding key.

Routing information is available from the set of public views:
 * LOCAL_CHUNK_TYPES view provides the information about the sharding type,
   as well as the name of the root sharded table,
 * LOCAL_CHUNK_COLUMNS view provides the sharding key columns and their types,
 * LOCAL_CHUNKS veiw provides the information about chunks available and 
   the ranges they store.

On the catalog database, `LOCAL_CHUNKS` view provides the information about all the chunks and

We propose a lightweight Java library for reading and manipulating that information.
It can be used to develop streaming applications which would take into
account Oracle Sharded Database data distribution.


## Library

### Getting Metadata

`ShardConfigurationInfo` caches the full or partial routing information.
The data can be loaded from stream or from the database.

### Routing Map



## Examples

```
    create tablespace set default datafile template autoextend on;

    create sharded table log(
      cust_id varchar2(128),
      ip_addr varchar2(128),
      hits integer,
      primary key(cust_id, ip_addr))
      partition by consistent hash (cust_id) 
        tablespace set default;
```


### Splitting Data Into Files

## Evaluations

## Case Study

## References
