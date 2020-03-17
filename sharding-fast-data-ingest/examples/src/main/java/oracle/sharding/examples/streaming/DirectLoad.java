/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.streaming;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.examples.common.Parameters;
import oracle.sharding.examples.common.ThreadLocalRandomSupplier;
import oracle.sharding.examples.loading.DirectPathLoadSinkCounted;
import oracle.sharding.examples.common.RoutingDataHelper;
import oracle.sharding.examples.schema.DemoLogEntry;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.ThreadBasedPartition;
import oracle.sharding.tools.DirectPathLoadSink;
import oracle.sharding.tools.SeparatedString;

import java.util.stream.Stream;

/**
 * In this example, we generate a series of entries,
 * which we then loads batches of entries into the database
 * in parallel using OCI Direct Path API.
 */
public class DirectLoad {
    public String tableName = "LOG";
    public String rootTableName = "LOG";

    public void run() throws Exception {
        /* Load the routing table from the catalog or file */
        ChunkTable routingTable = RoutingDataHelper.loadRoutingData().createChunkTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<SeparatedString> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.part(0)));

            engine.setCreateSinkFunction(chunk -> createLoader((Chunk) chunk));

            ThreadLocalRandomSupplier random = new ThreadLocalRandomSupplier();

            /* Generate some demo objects and break  */
            Stream.generate(() -> DemoLogEntry.generate(random))
                    .limit(Parameters.entriesToGenerate).parallel()
                    .map(x -> new SeparatedString(x.getCustomerId() + '|' +
                            x.getIpAddress() + '|' + x.getHits(), '|', 3))
                    .forEach((x) -> engine.getSplitter().feed(x));
        } finally {
            /* Flush all buffers */
            engine.getSplitter().closeAllInputs();

            /* Wait for all writing threads to finish */
            engine.waitAndClose(10240);
        }
    }

    private DirectPathLoadSink createLoader(Chunk chunk) {
        return new DirectPathLoadSinkCounted(new DirectPathLoadSink.Builder(
                chunk.getShard().getConnectionString(),
                    Parameters.username, Parameters.password)
            .setTarget(Parameters.schemaName, tableName, rootTableName + "_P" + chunk.getChunkId())
            .column("CUST_ID", 128)
            .column("IP_ADDR", 128)
            .column("HITS", 64));
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new DirectLoad().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
