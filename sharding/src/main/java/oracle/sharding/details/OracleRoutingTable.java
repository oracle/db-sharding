/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by itaranov on 4/5/17.
 */
public abstract class OracleRoutingTable implements RoutingTable<Chunk> {
    protected final OracleShardingMetadata metadata;

    protected OracleRoutingTable(OracleShardingMetadata metadata) {
        this.metadata = metadata;
    }

    public Optional<Chunk> getAnyChunk(boolean writable, Object... a) throws SQLException {
        Stream<Chunk> stream = streamLookup(metadata.createKey(a));
        if (writable) { stream = stream.filter(x -> x.getStatus() == 0); }
        return stream.findAny();
    }

    public Optional<Chunk> getAnyWritableChunk(Object... a) throws SQLException {
        return getAnyChunk(true, a);
    }

    public OracleShardingMetadata getMetadata() {
        return metadata;
    }



    public void atomicUpdate(Collection<Chunk> removeChunks, Collection<Chunk> updateChunks) {
        atomicUpdate(removeChunks, updateChunks, Chunk::getKeySet);
    }

    public RoutingKey createKey(Object ... a) {
        try {
            return metadata.createKey(a);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
