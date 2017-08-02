package oracle.sharding.sql;

import java.io.Serializable;
import java.sql.Connection;

/**
 * Created by somestuff on 4/6/17.
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
