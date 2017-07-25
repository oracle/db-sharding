package oracle.sharding.splitter;

/**
 * Created by somestuff on 6/20/17.
 */
public class OverflowPolicy<ItemT> {
    public static OverflowPolicy CREATE_SINK   = new OverflowPolicy();
    public static OverflowPolicy WAIT_FOR_SINK = new OverflowPolicy();

    private OverflowPolicy() {
    }

    public Object apply(Object container, ItemT batch) {
        return this;
    }
}
