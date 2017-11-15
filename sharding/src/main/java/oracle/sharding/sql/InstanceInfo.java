/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import java.io.Serializable;
import java.sql.Connection;

/**
 * Created by itaranov on 4/6/17.
 */
public class InstanceInfo implements Serializable {
    private final String shardName;
    private final String instanceName;
    final boolean isCatalog;
    String connectionString;
    transient boolean isPrimary;

    public InstanceInfo(String shardName, String instanceName) {
        this.shardName = shardName;
        this.instanceName = instanceName;
        this.isCatalog = false;
    }

    public InstanceInfo(String shardName, String instanceName, String connectionString) {
        this.shardName = shardName;
        this.instanceName = instanceName;
        this.connectionString = connectionString;
        this.isCatalog = false;
    }

    public InstanceInfo(Connection connection) {
        this.shardName = "";
        this.instanceName = "";
        this.isCatalog = true;
    }

    public String getShardName() {
        return shardName;
    }

    public String getInstanceName() { return instanceName; }

    public String getConnectionString() {
        return connectionString;
    }
}
