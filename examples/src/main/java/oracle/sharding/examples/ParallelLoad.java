package oracle.sharding.examples;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleRoutingTable;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.ThreadBasedPartition;
import oracle.sharding.tools.StatementSink;

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
            return DriverManager.getConnection("jdbc:oracle:thin:@" + connectionString, Common.username, Common.password)
                    .prepareStatement("insert /*+ append_values */ into log(cust_id, ip_addr, hits) values (?,?,?)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void bindLogEntry(DemoLogEntry entry, PreparedStatement statement) {
        try {
            statement.setString(1, entry.getCustomerId());
            statement.setString(2, entry.getIpAddress());
            statement.setLong(3, entry.getHits());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() throws Exception {
        /* Load the routing table from the catalog or file */
        OracleRoutingTable routingTable = RoutingDataSerialization.loadRoutingData().createRoutingTable();

        /* Create a batching partitioning engine based on the catalog */
        PartitionEngine<DemoLogEntry> engine = new ThreadBasedPartition<>(routingTable);

        try {
            /* Provide a function, which get the key given an object */
            engine.setKeyFunction(a -> routingTable.createKey(a.getCustomerId()));

            engine.setCreateSinkFunction(chunk ->
                    new StatementSink<>(() -> this.createInsertStatement(
                        ((Chunk) chunk).getShard().getConnectionString()), this::bindLogEntry));

            /* Generate some demo objects and break  */
            Stream.generate(DemoLogEntry::generate).limit(100000).parallel()
                    .forEach((x) -> engine.getSplitter().feed(x));
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
            new ParallelLoad().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
