package oracle.sharding.splitter;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by somestuff on 7/19/17.
 */
public abstract class PartitionEngine<ItemT> implements AutoCloseable {
    protected final GeneralSplitter<ItemT> splitter;
    protected Function<Object, Consumer<List<ItemT>>> createSinkFunction = null;

    public void setCreateSinkFunction(Function<Object, Consumer<List<ItemT>>> createSinkFunction) {
        this.createSinkFunction = createSinkFunction;
    }

    public Function<Object, Consumer<List<ItemT>>> getCreateSinkFunction() {
        return createSinkFunction;
    }

    public GeneralSplitter<ItemT> getSplitter() {
        return splitter;
    }

    public PartitionEngine(GeneralSplitter<ItemT> splitter) {
        this.splitter = splitter;
    }

    public abstract void createSink(Object chunk, Consumer<List<ItemT>> sink);
    public abstract void waitAndClose(long waitTimeout) throws Exception;
}
