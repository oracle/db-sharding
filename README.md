# Oracle Sharding Tools Library

This library provides Oracle Sharding routing implementation for use in client applications.

## Motivation

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
