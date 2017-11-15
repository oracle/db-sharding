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
import oracle.sharding.SetOfKeys;

import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by itaranov on 4/4/17.
 */
public class CompositeRoutingTable extends OracleRoutingTable {
    private RoutingTable<ChunkGroup> superLevelRoutingTable;

    @Override
    public Stream<Chunk> streamLookup(RoutingKey _key) {
        CompositeRoutingKey key = (CompositeRoutingKey) _key;

        return superLevelRoutingTable.streamLookup(key.getSuperShardingKey()).flatMap(
                x -> x.routingTable.streamLookup(key.getShardingKey()));
    }

    @Override
    public boolean isEmpty() {
        return superLevelRoutingTable.isEmpty();
    }

    @Override
    public void atomicUpdate(Collection<Chunk> removeChunks, Collection<Chunk> updateChunks, Function<Chunk, SetOfKeys> getKeySet) {

    }

    @Override
    public Collection<Chunk> values() {
        return null;
    }

    private class ChunkGroup {
        private RoutingTable<Chunk> routingTable;
    }

    public CompositeRoutingTable(OracleShardingMetadata metadata) throws SQLException {
        super(metadata);
        this.superLevelRoutingTable = SimpleRoutingTableWrapper.createRoutingTable(metadata.getSuperShardingMetadata());
    }
}
