package oracle.sharding.splitter;

import oracle.sharding.RoutingKey;

/**
 * Created by somestuff on 6/9/17.
 */
public interface Feeder<ItemT> extends AutoCloseable {
    void feed(ItemT item);
    void feed(RoutingKey key, ItemT item);
    void flush();
}
