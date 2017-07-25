package oracle.sharding.details;

import oracle.sharding.RoutingKey;
import oracle.sharding.SimpleRoutingKey;

import java.util.Comparator;

/**
 * Created by somestuff on 6/23/17.
 */
public class CompositeRoutingKey extends RoutingKey {
    private final SimpleRoutingKey superShardingKey;
    private final SimpleRoutingKey shardingKey;

    public CompositeRoutingKey(OracleShardingMetadata metadata, SimpleRoutingKey superShardingKey, SimpleRoutingKey shardingKey) {
        super(metadata);
        this.superShardingKey = superShardingKey;
        this.shardingKey = shardingKey;
    }

    public SimpleRoutingKey getSuperShardingKey() {
        return superShardingKey;
    }

    public SimpleRoutingKey getShardingKey() {
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

    private static Comparator<SimpleRoutingKey> keyComparator =
            Comparator.nullsFirst(SimpleRoutingKey::compareTo);

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
