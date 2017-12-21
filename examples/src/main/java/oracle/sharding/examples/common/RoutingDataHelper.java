/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.common;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.sql.MetadataReader;
import oracle.sharding.sql.ShardConfiguration;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Class contains static methods to help read routing information
 * from the catalog or files
 */
public class RoutingDataHelper {
    /**
     * Load the shard configuration from file
     *
     * @param file to load from
     * @return Shard configuration
     * @throws IOException passed from open file
     * @throws ClassNotFoundException serialization exception
     */
    public static ShardConfiguration loadFromFile(File file)
            throws IOException, ClassNotFoundException
    {
        try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(file))) {
            return (ShardConfiguration) output.readObject();
        }
    }

    /**
     * Load the shard configuration from the file specified in config
     *
     * @return Shard configuration
     * @throws IOException passed
     * @throws ClassNotFoundException passed
     */
    public static ShardConfiguration loadFromDefaultFile()
            throws IOException, ClassNotFoundException
    {
        if (Parameters.routingTableFile != null) {
            return loadFromFile(new File(Parameters.routingTableFile));
        } else {
            throw new IOException("File not specified");
        }
    }

    /**
     * Load the shard configuration from the catalog database
     *
     * @return shard configuration
     * @throws SQLException in case
     */
    public static ShardConfiguration loadFromDefaultCatalog()
            throws SQLException
    {
        /* Create connection to the catalog */
        try (Connection connection = Parameters.getCatalogConnection())
        {
            /* Load the routing table from the catalog */
            return new MetadataReader(connection).getShardConfiguration();
        }
    }

    public ShardConfiguration connectAndSave() throws SQLException, IOException
    {
        ShardConfiguration result;

        /* Create connection to the catalog */
        try (Connection connection = Parameters.getCatalogConnection())
        {
            /* Load the routing table from the catalog */
            result = new MetadataReader(connection).getShardConfiguration();
        }

        if (Parameters.routingTableFile != null) {
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Parameters.routingTableFile))) {
                output.writeObject(result);
            }
        }

        return result;
    }

    public static ShardConfiguration getShardConfiguration() throws IOException, ClassNotFoundException, SQLException
    {
        if (Parameters.routingTableFile != null && new File(Parameters.routingTableFile).isFile()) {
            try (ObjectInputStream output = new ObjectInputStream(new FileInputStream(Parameters.routingTableFile))) {
                return ((ShardConfiguration) output.readObject());
            }
        } else {
            ShardConfiguration result;

            /* Create connection to the catalog */
            try (Connection connection = DriverManager
                    .getConnection(Parameters.catalogConnectString, Parameters.username, Parameters.password))
            {
                /* Load the routing table from the catalog */
                result = new MetadataReader(connection).getShardConfiguration();
            }

            if (Parameters.routingTableFile != null) {
                ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Parameters.routingTableFile));
                output.writeObject(result);
            }

            return result;
        }
    }

    public static OracleShardingMetadata loadRoutingData()
            throws IOException, ClassNotFoundException, SQLException
    {
        return getShardConfiguration().createMetadata();
    }

    public static ChunkTable getChunkTable()
            throws IOException, ClassNotFoundException, SQLException
    {
        return loadRoutingData().createChunkTable();
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new RoutingDataHelper().connectAndSave();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
