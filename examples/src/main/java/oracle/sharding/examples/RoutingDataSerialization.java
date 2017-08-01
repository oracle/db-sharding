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
    private static final String connectionString = "jdbc:oracle:thin:@" +
            "(DESCRIPTION=(ADDRESS=(HOST=10.242.154.91)(PORT=1522)(PROTOCOL=tcp))" +
            "(CONNECT_DATA=(SERVICE_NAME=GDS$CATALOG.ORADBCLOUD)))";

    private static final String username = "dyn";
    private static final String password = "123";

    private static final String routingTableFile = "routing.dat";

    public ShardConfigurationInfo connectAndSave() throws SQLException, IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(routingTableFile))) {
            ShardConfigurationInfo result;

        /* Create connection to the catalog */
            try (Connection connection = DriverManager.getConnection(connectionString, username, password)) {
            /* Load the routing table from the catalog */
                result = ShardConfigurationInfo.loadFromDatabase(connection);
                output.writeObject(result);
            }

            return result;
        }
    }

    public ShardConfigurationInfo load() throws IOException, ClassNotFoundException {
        try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(routingTableFile))) {
            return (ShardConfigurationInfo) output.readObject();
        }
    }

    public ShardConfigurationInfo loadFromFileOrCatalog()
            throws IOException, ClassNotFoundException, SQLException
    {
        if (new File(routingTableFile).isFile()) {
            return load();
        } else {
            return connectAndSave();
        }
    }

    public static ShardConfigurationInfo loadRoutingData()
            throws IOException, ClassNotFoundException, SQLException
    {
        if (new File(routingTableFile).isFile()) {
            try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(routingTableFile))) {
                return (ShardConfigurationInfo) output.readObject();
            }
        } else {
            /* Create connection to the catalog */
            try (Connection connection = DriverManager.getConnection(connectionString, username, password);
                 ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(routingTableFile)))
            {
                /* Load the routing table from the catalog */
                ShardConfigurationInfo result = ShardConfigurationInfo.loadFromDatabase(connection);
                output.writeObject(result);
                return result;
            }
        }
    }

    public static void main(String [] args)
    {
        try {
            new File(routingTableFile).delete();
            new RoutingDataSerialization().connectAndSave();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
