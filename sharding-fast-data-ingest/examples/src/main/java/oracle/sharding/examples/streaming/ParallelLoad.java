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
import oracle.sharding.examples.common.ParallelGenerator;
import oracle.sharding.examples.common.Parameters;
import oracle.sharding.examples.common.ThreadLocalRandomSupplier;
import oracle.sharding.examples.common.RoutingDataHelper;
import oracle.sharding.examples.schema.DemoLogEntry;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.ThreadBasedPartition;
import oracle.sharding.tools.StatementSink;
import oracle.util.metrics.Statistics;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * In this example, we generate a series of entries,
 * which we then loads batches of entries into the database
 * in parallel using regular insert + append_values statement.
 */
public class ParallelLoad {
    private PreparedStatement createInsertStatement(String connectionString) {
        try {
            return DriverManager.getConnection("jdbc:oracle:thin:@" + connectionString, Parameters.username, Parameters.password)
                    .prepareStatement("insert /*+ append_values */ into log(cust_id, ip_addr, hits) values (?,?,?)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Statistics.PerformanceMetric metric = Statistics.getGlobal()
            .createPerformanceMetric("QueryInserts", Statistics.PER_SECOND);

    private void bindLogEntry(DemoLogEntry entry, PreparedStatement statement) {
        try {
            statement.setString(1, entry.getCustomerId());
            statement.setString(2, entry.getIpAddress());
            statement.setLong(3, entry.getHits());
            metric.inc();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() throws Exception {
        /* Load the routing table from the catalog or file */
        ChunkTable routingTable = RoutingDataHelper.getChunkTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<DemoLogEntry> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.getCustomerId()));

            engine.setCreateSinkFunction(chunk ->
                    new StatementSink<>(() -> this.createInsertStatement(
                        ((Chunk) chunk).getShard().getConnectionString()), this::bindLogEntry));

            new ParallelGenerator(() -> () -> {
                ThreadLocalRandomSupplier random = new ThreadLocalRandomSupplier();

                Stream.generate(() -> DemoLogEntry.generate(random))
                        .limit(Parameters.entriesToGenerate / Parameters.parallelThreads)
                        .forEach((x) -> engine.getSplitter().feed(x));

                engine.getSplitter().flush();
            }).execute(Parameters.parallelThreads).awaitTermination();

            /* Generate some demo objects and break  */
            /*
            Stream.generate(DemoLogEntry::generate).limit(Parameters.entriesToGenerate).parallel()
                    .forEach((x) -> engine.getSplitter().feed(x));
            */
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
            new ParallelLoad().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
