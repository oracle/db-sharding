/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.sorting;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.RoutingTable;
import oracle.sharding.details.Chunk;
import oracle.sharding.examples.common.ClosableSet;
import oracle.sharding.examples.common.Parameters;
import oracle.sharding.examples.schema.DemoLogEntry;
import oracle.sharding.sql.MetadataReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;

import static oracle.sharding.examples.common.Parameters.outputDirectory;

/**
 * Simple example to sort the data over chunks.
 *
 * This application reads the information on chunks from
 */
public class CustomDataSorting {
    OracleShardingMetadata metadata;
    RoutingTable<Writer> routingTable;

    void run() throws SQLException, IOException, ClassNotFoundException
    {
        loadRoutingTable();
        fillRoutingTable();

        try (ClosableSet allWriters = new ClosableSet(routingTable.values())) {
            for (int i = 0; i < Parameters.entriesToGenerate; ++i) {
                DemoLogEntry entry = DemoLogEntry.generate();

                routingTable.find(metadata.createKey(entry.getCustomerId())).iterator().next()
                        .write(entry.toString() + "\n");
            }
        }
    }

    void loadRoutingTable() throws IOException, SQLException
    {
        /* Create connection to the catalog */
        try (Connection connection = Parameters.getCatalogConnection())
        {
            /* Load the routing table from the catalog */
            metadata = new MetadataReader(connection).getMetadata();
        }

        routingTable = metadata.createRoutingTable();
    }

    void fillRoutingTable() throws IOException {
        RoutingTable.RoutingTableModifier<Writer> routingTableUpdater = routingTable.modifier();

        /* Create a file per chunk */
        for (Chunk chunk : metadata.getAllChunks()) {
            if (chunk.isPrimary()) {
                routingTableUpdater.add(
                    new BufferedWriter(new FileWriter(outputDirectory + "chunk_" + chunk.getUniqueId() + ".txt")),
                    chunk.getKeySet());
            }
        }

        routingTableUpdater.clearAndSet();
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new CustomDataSorting().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
