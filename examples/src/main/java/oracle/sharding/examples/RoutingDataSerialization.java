package oracle.sharding.examples;

import oracle.sharding.sql.ShardConfigurationInfo;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by somestuff on 7/31/17.
 */
public class RoutingDataSerialization {
    public ShardConfigurationInfo connectAndSave() throws SQLException, IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Common.routingTableFile))) {
            ShardConfigurationInfo result;

        /* Create connection to the catalog */
            try (Connection connection = DriverManager.getConnection(
                    Common.connectionString, Common.username, Common.password))
            {
            /* Load the routing table from the catalog */
                result = ShardConfigurationInfo.loadFromDatabase(connection, true);
                output.writeObject(result);
            }

            return result;
        }
    }

    public ShardConfigurationInfo load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(Common.routingTableFile)))
        {
            return (ShardConfigurationInfo) output.readObject();
        }
    }

    public ShardConfigurationInfo loadFromFileOrCatalog()
            throws IOException, ClassNotFoundException, SQLException
    {
        if (new File(Common.routingTableFile).isFile()) {
            return load();
        } else {
            return connectAndSave();
        }
    }

    public static ShardConfigurationInfo loadRoutingData()
            throws IOException, ClassNotFoundException, SQLException
    {
        if (new File(Common.routingTableFile).isFile()) {
            try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(Common.routingTableFile))) {
                return (ShardConfigurationInfo) output.readObject();
            }
        } else {
            /* Create connection to the catalog */
            try (Connection connection = DriverManager.getConnection(Common.connectionString, Common.username, Common.password);
                 ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Common.routingTableFile)))
            {
                /* Load the routing table from the catalog */
                ShardConfigurationInfo result = ShardConfigurationInfo.loadFromDatabase(connection, true);
                output.writeObject(result);
                return result;
            }
        }
    }

    public static void main(String [] args)
    {
        try {
            new File(Common.routingTableFile).delete();
            new RoutingDataSerialization().connectAndSave();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
