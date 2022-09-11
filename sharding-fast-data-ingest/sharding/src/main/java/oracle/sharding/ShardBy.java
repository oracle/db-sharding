/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

/**
 * Enumeration with different types of sharding
 */
public enum ShardBy {
    /* Shard by hash (consistent OR partition hash) */
    HASH(1),

    /* Shard by range */
    RANGE(2),

    /* Shard by list */
    LIST(4),

    /* Sharding level not used */
    NONE(0);

    /* Id corresponds to internal oracle identifiers for both partitioning and sharding */
    int id;
    ShardBy(int aid) { id = aid; }
}
