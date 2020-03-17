/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding.examples.sorting;

import oracle.sharding.RoutingTable;
import oracle.sharding.details.Chunk;
import oracle.sharding.examples.common.Parameters;
import oracle.util.function.FunctionWithError;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static oracle.sharding.examples.common.Parameters.outputDirectory;

/**
 * Simple example to sort the data over shards.
 */
public class DataSortingByShard extends CustomDataSorting {
    void fillRoutingTable() {
        RoutingTable.RoutingTableModifier<Writer> routingTableUpdater = routingTable.modifier();
        Map<String, Writer> files = new HashMap<>();

        /* Create a file per shard */
        for (Chunk chunk : metadata.getAllChunks()) {
            if (chunk.isPrimary()) {
                String shardName = chunk.getShard().getName();

                routingTableUpdater.add(
                        files.computeIfAbsent(shardName,
                                FunctionWithError.create(name -> new BufferedWriter(
                                        new FileWriter(outputDirectory + "shard_" + name + ".txt")))
                                        .onErrorRuntimeException()),
                        chunk.getKeySet());
            }
        }

        routingTableUpdater.clearAndSet();
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new DataSortingByShard().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
