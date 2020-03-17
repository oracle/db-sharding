/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.OracleShardingMetadata;

import java.util.Comparator;

/**
 * Composite key
 */
public class CompositeRoutingKey implements RoutingKey {
    private final RoutingKey superShardingKey;
    private final RoutingKey shardingKey;

    public CompositeRoutingKey(OracleShardingMetadata metadata, RoutingKey superShardingKey, RoutingKey shardingKey) {
//        super(metadata);
        this.superShardingKey = superShardingKey;
        this.shardingKey = shardingKey;
    }

    public RoutingKey getSuperShardingKey() {
        return superShardingKey;
    }

    public RoutingKey getShardingKey() {
        return shardingKey;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompositeRoutingKey
                && (superShardingKey == null ? (((CompositeRoutingKey) obj).superShardingKey == null)
                    : superShardingKey.equals(((CompositeRoutingKey) obj).superShardingKey))
                && (shardingKey == null ? (((CompositeRoutingKey) obj).shardingKey == null)
                    : shardingKey.equals(((CompositeRoutingKey) obj).shardingKey));
    }

    private static Comparator<RoutingKey> keyComparator =
            Comparator.nullsFirst(RoutingKey::compareTo);

    public static Comparator<CompositeRoutingKey> superKeyComparator =
            Comparator.comparing(CompositeRoutingKey::getSuperShardingKey, keyComparator);

    public static Comparator<CompositeRoutingKey> shardingKeyComparator =
            Comparator.comparing(CompositeRoutingKey::getShardingKey, keyComparator);

    public static Comparator<CompositeRoutingKey> fullComparator =
            Comparator.nullsFirst(superKeyComparator)
                    .thenComparing(shardingKeyComparator);

    @Override
    public int compareTo(RoutingKey other) {
        return fullComparator.compare(this, (CompositeRoutingKey) other);
    }
}
