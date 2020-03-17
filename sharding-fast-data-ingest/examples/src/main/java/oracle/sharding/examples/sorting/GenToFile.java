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
import oracle.sharding.tools.UnbatchingSink;
import oracle.util.metrics.Statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static oracle.sharding.examples.common.Parameters.outputDirectory;

/**
 * Example of partitioning.
 */
public class GenToFile {
    /* Create a function which writes  */
    public Consumer<List<DemoLogEntry>> createOrderSink(String filename) {
        try {
            return UnbatchingSink.unbatchToStrings(
                new BufferedWriter(new FileWriter(filename)),
                    demoLogEntry -> {
                        metric.inc();
                        return demoLogEntry.toString();
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private final static Statistics.PerformanceMetric metric = Statistics.getGlobal()
            .createPerformanceMetric("FileWrites", Statistics.PER_SECOND);

    public void run() throws Exception {
        /* Load the routing table from the catalog or file */
        ChunkTable routingTable = RoutingDataHelper.getChunkTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<DemoLogEntry> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which writes the data for each chunk to a separate file */
            engine.setCreateSinkFunction(
                    chunk -> createOrderSink(outputDirectory + "test-CHUNK_" + ((Chunk) chunk).getUniqueId()));

            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.getCustomerId()));

            new ParallelGenerator(() -> () -> {
                ThreadLocalRandomSupplier random = new ThreadLocalRandomSupplier();

                Stream.generate(() -> DemoLogEntry.generate(random))
                        .limit(Parameters.entriesToGenerate / Parameters.parallelThreads)
                        .forEach((x) -> engine.getSplitter().feed(x));

                engine.getSplitter().flush();
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
            new GenToFile().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
