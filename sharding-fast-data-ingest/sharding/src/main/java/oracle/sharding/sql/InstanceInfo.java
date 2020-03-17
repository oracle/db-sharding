/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import java.io.Serializable;

/**
 * Shard information from SHA_DATABASE
 */
public class InstanceInfo implements Serializable {
    private final String shardName;
    private String connectionString;
    private boolean isPrimary;

    public InstanceInfo(String shardName) {
        this.shardName = shardName;
    }

    public InstanceInfo(String shardName, String connectionString) {
        this.shardName = shardName;
        this.connectionString = connectionString;
    }

    public String getShardName() {
        return shardName;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public boolean isPrimary() {
        return isPrimary;
    }
}
