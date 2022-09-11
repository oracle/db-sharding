/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Data partitioner
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

    public void setKeyFunction(Function<ItemT, RoutingKey> function) {
        splitter.setGetKey(function);
    }

    public RoutingTable getRoutingTable() {
        return splitter.getRoutingTable();
    }
}
