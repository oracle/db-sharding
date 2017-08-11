package oracle.sharding.examples;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleRoutingTable;
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
        OracleRoutingTable routingTable = RoutingDataSerialization.loadRoutingData().createRoutingTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<SeparatedString> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.part(0)));

            engine.setCreateSinkFunction(chunk -> createLoader((Chunk) chunk));

            /* Generate some demo objects and break  */
            Stream.generate(DemoLogEntry::generate).limit(Parameters.entriesToGenerate).parallel()
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
        return new DirectPathLoadSink.Builder(
                chunk.getShard().getConnectionString(),
                    Parameters.username, Parameters.password)
            .setTarget(Parameters.schemaName, tableName, rootTableName + "_P" + chunk.getChunkId())
            .column("CUST_ID", 128)
            .column("IP_ADDR", 128)
            .column("HITS", 64)
            .build();
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new ParallelLoad().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
