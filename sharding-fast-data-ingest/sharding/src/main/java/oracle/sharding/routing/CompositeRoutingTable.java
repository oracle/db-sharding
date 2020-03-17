/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;
import oracle.sharding.OracleShardingMetadata;

import java.sql.SQLException;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Two-level routing table
 */
public class CompositeRoutingTable<T> implements RoutingTable<T> {
    private final OracleShardingMetadata metadata;
    private RoutingTable<Group> superLevelRoutingTable;

    @Override
    public Stream<T> streamLookup(RoutingKey _key) {
        CompositeRoutingKey key = (CompositeRoutingKey) _key;

        return superLevelRoutingTable.streamLookup(key.getSuperShardingKey()).flatMap(
                x -> x.routingTable.streamLookup(key.getShardingKey()));
    }

    @Override
    public boolean isEmpty() {
        return superLevelRoutingTable.isEmpty();
    }

    @Override
    public Collection<T> values() {
        return null;
    }

    @Override
    public RoutingTableModifier<T> modifier() {
        return null;
    }

    private class Group {
        private final RoutingTable<T> routingTable;

        public Group() {
            this.routingTable = OracleShardingMetadata
                    .createSimpleRoutingTable(metadata.getShardingMetadata());
        }
    }

    public CompositeRoutingTable(OracleShardingMetadata metadata) throws SQLException {
        this.metadata = metadata;
        this.superLevelRoutingTable = OracleShardingMetadata
                .createSimpleRoutingTable(metadata.getSuperShardingMetadata());
    }
}
