/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding;

import junit.framework.TestCase;
import oracle.sharding.details.Chunk;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.details.OracleKeyColumn;
import oracle.sharding.details.Shard;
import oracle.sharding.routing.ConsistentHashRoutingTest.TestChunk;

import java.util.List;

import static oracle.sharding.routing.ConsistentHashRoutingTest.createRealisticChunks;
import static oracle.sql.CharacterSet.UTF8_CHARSET;

/**
 * Test for Sharding Metadata
 */
public class OracleShardingMetadataTest extends TestCase {
    public void testMetadataBuilder() throws Exception {
        OracleShardingMetadata.Builder osmBuilder =
                OracleShardingMetadata.builder().setShardingType(ShardBy.HASH);

        osmBuilder.addShardColumn(OracleKeyColumn
                .createOracleKeyColumn(OracleKeyColumn.DTY_VARCHAR, UTF8_CHARSET, 40));

        OracleShardingMetadata osm = osmBuilder.build();
        ChunkTable routingTable = osm.createChunkTable();

        List<TestChunk> chunkList = createRealisticChunks(100);

        assertFalse("Initially no keys", routingTable.getAnyChunk("key1").isPresent());

        Shard shard = osm.getShardForUpdate("db1");

        for (TestChunk tchunk : chunkList.subList(0, 50)) {
            Chunk chunk = new Chunk(tchunk.id, shard, 0, tchunk.keys);
            chunk.annotation = tchunk;
            shard.addChunk(chunk);
        }

        shard = osm.getShardForUpdate("db2");

        for (TestChunk tchunk : chunkList.subList(50, 100)) {
            Chunk chunk = new Chunk(tchunk.id, shard, 0, tchunk.keys);
            chunk.annotation = tchunk;
            shard.addChunk(chunk);
        }

        osm.updateChunkTables();

        assertEquals(routingTable.getAnyChunk("key1").get().annotation, chunkList.get(62));
        assertEquals(routingTable.getAnyChunk("keya").get().annotation, chunkList.get(53));
        assertEquals(routingTable.getAnyChunk("bg4").get().annotation, chunkList.get(12));
        assertEquals(routingTable.getAnyChunk("c").get().annotation, chunkList.get(33));
        assertEquals(routingTable.getAnyChunk("suihuh").get().annotation, chunkList.get(59));
    }
}
