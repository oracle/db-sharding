/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.sorting;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.examples.common.ParallelGenerator;
import oracle.sharding.examples.common.Parameters;
import oracle.sharding.examples.common.ThreadLocalRandomSupplier;
import oracle.sharding.examples.common.RoutingDataHelper;
import oracle.sharding.examples.schema.DemoLogEntry;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.ThreadBasedPartition;

import java.util.stream.Stream;

import static oracle.sharding.examples.common.Parameters.outputDirectory;

public class GenToFileStrings {
    public void run() throws Exception {
        /* Load the routing table from the catalog or file */
        ChunkTable routingTable = RoutingDataHelper.loadRoutingData().createChunkTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<String> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which writes the data for each chunk to a separate file */
            engine.setCreateSinkFunction(
                    chunk -> new FileWriterCounted(outputDirectory + "test-strings-CHUNK_" + ((Chunk) chunk).getUniqueId()));

            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.substring(0, a.indexOf(','))));

            new ParallelGenerator(() -> () -> {
                ThreadLocalRandomSupplier random = new ThreadLocalRandomSupplier();

                Stream.generate(() -> DemoLogEntry.generateString(random))
                        .limit(Parameters.entriesToGenerate / Parameters.parallelThreads)
                        .forEach((x) -> engine.getSplitter().feed(x));
            }).execute(Parameters.parallelThreads).awaitTermination();
        } finally {
            /* Flush all buffers */
            engine.getSplitter().closeAllInputs();

            /* Wait for all writing threads to finish */
            engine.waitAndClose(10240);
        }
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new GenToFileStrings().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
