package oracle.sharding.examples;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleRoutingTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple example to sort the data over chunks
 */
public class DataSorting {
    private static Writer createChunkWriter(Chunk chunk) {
        try {
            return new BufferedWriter(new FileWriter("chunk_" + chunk.getChunkUniqueId() + ".txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() throws SQLException, IOException, ClassNotFoundException {
        final OracleRoutingTable routingTable =
                RoutingDataSerialization.loadRoutingData().createRoutingTable();
        final Map<Chunk, Writer> writers = new HashMap<>();

        try {
            for (int i = 0; i < 1000000; ++i) {
                DemoLogEntry entry = DemoLogEntry.generate();
                Chunk chunk = routingTable.getAnyWritableChunk(entry.getCustomerId()).orElse(null);

                if (chunk != null) {
                    writers.computeIfAbsent(chunk, DataSorting::createChunkWriter)
                            .append(entry.toString()).append('\n');
                }
            }
        } finally {
            for (Writer wr : writers.values()) {
                try { wr.close(); } catch (IOException ignore) { };
            }
        }
    }

    public static void main(String [] args)
    {
        try {
            new DataSorting().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
