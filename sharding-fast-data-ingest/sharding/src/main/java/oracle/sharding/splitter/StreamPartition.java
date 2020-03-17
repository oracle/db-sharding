/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

import oracle.sharding.RoutingTable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

class StreamPartition<ItemT> {
    private int queueSize = 1024;
    protected final GeneralSplitter<ItemT> splitter;
    private final Map<Object, ConsumerQueue<List<ItemT>>> chunkQueues = new ConcurrentHashMap<>();
    protected BiConsumer<Object, Stream<List<ItemT>>> createSinkFunction = null;

    public StreamPartition(RoutingTable routingTable,
        BiConsumer<Object, Stream<List<ItemT>>> createSinkFunction)
    {
        this.splitter = new GeneralSplitter<ItemT>(routingTable);
        this.createSinkFunction = createSinkFunction;
        splitter.setSink(this::acceptItem);
    }

    private void acceptItem(Object container, List<ItemT> batch) {
        chunkQueues.computeIfAbsent(container, o -> createContainerQueue(container)).accept(batch);
    }

    private ConsumerQueue<List<ItemT>> createContainerQueue(Object container) {
        ConsumerQueue<List<ItemT>> result = new ConsumerQueue<>(queueSize);
        createSinkFunction.accept(container, result.stream());
        return result;
    }

    public StreamPartition(RoutingTable routingTable)
    {
        this.splitter = new GeneralSplitter<ItemT>(routingTable);
    }

    public void setCreateSinkFunction(BiConsumer<Object, Stream<List<ItemT>>> createSinkFunction) {
        this.createSinkFunction = createSinkFunction;
    }
}
