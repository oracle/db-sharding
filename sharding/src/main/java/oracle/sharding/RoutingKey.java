package oracle.sharding;

/**
 * Created by somestuff on 4/5/17.
 */
public abstract class RoutingKey implements Comparable<RoutingKey> {
    protected final RoutingMetadata metadata;

    public RoutingKey(RoutingMetadata metadata) {
        this.metadata = metadata;
    }
}
