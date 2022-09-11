/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Direct routing table wrapper, which maps keys to chunks.
 */
public class ChunkTable implements RoutingTable<Chunk> {
    protected final OracleShardingMetadata metadata;
    private final RoutingTable<Chunk> subject;

    /**
     * Create a routing table which maps keys to chunks and
     * update it with the known state of the routing metadata.
     *
     * @param metadata to use
     * @return a chunk table
     * @throws SQLException if the table cannot be created
     */
    public static ChunkTable create(OracleShardingMetadata metadata) throws SQLException
    {
        ChunkTable table = new ChunkTable(metadata);
        metadata.updateChunkTable(table);
        return table;
    }

    @Override
    public boolean isEmpty() {
        return subject.isEmpty();
    }

    @Override
    public Collection<Chunk> lookup(RoutingKey key) {
        return subject.lookup(key);
    }

    @Override
    public Collection<Chunk> lookup(RoutingKey key, Collection<Chunk> result) {
        return subject.lookup(key, result);
    }

    @Override
    public Stream<Chunk> streamLookup(RoutingKey key) {
        return subject.streamLookup(key);
    }

    @Override
    public RoutingTableModifier<Chunk> modifier() {
        return subject.modifier();
    }

    @Override
    public Collection<Chunk> values() {
        return subject.values();
    }

    /**
     * Internal method to create a chunk table tied to a particular metadata object
     * The constructor left public intentionally but is not expected to be used.
     * Use static create() method or OracleShardingMetadata::createChunkTable() method instead.
     *
     * @param metadata metadata object
     * @throws SQLException if the routing table cannot be created.
     */
    public ChunkTable(OracleShardingMetadata metadata) throws SQLException {
        this.metadata = metadata;
        this.subject = metadata.createRoutingTable();
    }

    public Optional<Chunk> getAnyChunk(Object... a) throws SQLException {
        return streamLookup(metadata.createKey(a)).findAny();
    }

    public Optional<Chunk> getWritableChunk(Object... a) throws SQLException {
        return streamLookup(metadata.createKey(a)).filter(Chunk::isWritable).findAny();
    }

    public OracleShardingMetadata getMetadata() {
        return metadata;
    }

    public RoutingKey createKey(Object ... a) {
        try {
            return metadata.createKey(a);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
