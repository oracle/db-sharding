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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Split input data based on the given routing table and batch them pipelining further.
 */
public class GeneralSplitter<ItemT> implements Feeder<ItemT> {
    private final RoutingTable routingTable;
    private Function<ItemT, RoutingKey> getKey = null;
    private BiConsumer<Object, List<ItemT>> sink;
    private Function<RoutingKey, Object> partitionFunction;
    private int bucketSize = 1024;
    private Consumer<ItemT> partitionNotFoundPolicy = (Consumer<ItemT>) defaultPartitionNotFoundPolicy;

    private static Consumer<Object> defaultPartitionNotFoundPolicy =
            (x) -> { throw new RuntimeException("Partition not found for key : " + String.valueOf(x)); };

    public GeneralSplitter(RoutingTable routingTable) {
        this.routingTable = routingTable;
        this.partitionFunction = defaultPartitionFunction();
    }

    private Function<RoutingKey, Object> defaultPartitionFunction() {
        return key -> routingTable.streamLookup(key).findAny().orElse(null);
    }

    public Feeder<ItemT> createLocalFeeder()
    {
        if (closed) {
            throw new IllegalStateException("Splitter does not accept data anymore");
        }

        return new ThreadLocalFeeder();
    }

    public Function<ItemT, RoutingKey> getGetKey() {
        return getKey;
    }

    public GeneralSplitter<ItemT> setGetKey(Function<ItemT, RoutingKey> getKey) {
        this.getKey = getKey;
        return this;
    }

    public BiConsumer<Object, List<ItemT>> getSink() {
        return sink;
    }

    public GeneralSplitter<ItemT> setSink(BiConsumer<Object, List<ItemT>> sink) {
        this.sink = sink;
        return this;
    }

    public Function<RoutingKey, Object> getPartitionFunction() {
        return partitionFunction;
    }

    public GeneralSplitter<ItemT> setPartitionFunction(Function<RoutingKey, Object> partitionFunction) {
        this.partitionFunction = partitionFunction;

        if (this.partitionFunction == null) {
            this.partitionFunction = defaultPartitionFunction();
        }

        return this;
    }

    public GeneralSplitter<ItemT> extendPartitionFunction(Function<Object, Object> mapToSink) {
        this.partitionFunction = partitionFunction.andThen(mapToSink);
        return this;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public GeneralSplitter<ItemT> setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
        return this;
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    private abstract class GeneralFeeder implements Feeder<ItemT> {
        protected abstract int put(Object container, ItemT item);
        protected abstract void flush(Object container);

        @Override
        public void feed(ItemT item) {
            feed(getKey.apply(item), item);
        }

        @Override
        public void feed(RoutingKey key, ItemT item) {
            Object container = partitionFunction.apply(key);

            if (container != null)
            {
                if (put(container, item) >= bucketSize) {
                    flush(container);
                }
            }
            else
            {
                partitionNotFoundPolicy.accept(item);
            }
        }

        @Override
        public void close() throws Exception {
            flush();
        }
    }

    private class ThreadLocalFeeder extends GeneralFeeder {
        private final Map<Object, List<ItemT> > buckets = new HashMap<>();

        @Override
        public void flush() {
            buckets.forEach((k, v) -> sink.accept(k, v));
            buckets.clear();
        }

        @Override
        protected int put(Object container, ItemT item) {
            List<ItemT> bucket = buckets.computeIfAbsent(container, _ignore -> new ArrayList<>(bucketSize));
            bucket.add(item);
            return bucket.size();
        }

        @Override
        protected void flush(Object container) {
            List<ItemT> bucket = buckets.remove(container);
            sink.accept(container, bucket);
        }
    }

    private final ThreadLocal<ThreadLocalFeeder> localFeeders = new ThreadLocal<>();
    private final ConcurrentLinkedQueue<ThreadLocalFeeder> localFeederList = new ConcurrentLinkedQueue<>();
    private volatile boolean closed = false;

    private ThreadLocalFeeder getLocalFeeder() {
        ThreadLocalFeeder result = localFeeders.get();

        if (result == null) {
            synchronized (this) {
                localFeeders.set(result = new ThreadLocalFeeder());

                if (closed) {
                    throw new IllegalStateException("Splitter does not accept data anymore");
                }

                localFeederList.offer(result);
            }
        }

        return result;
    }

    @Override
    public void feed(ItemT item) {
        getLocalFeeder().feed(getKey.apply(item), item);
    }

    @Override
    public void feed(RoutingKey key, ItemT item) {
        getLocalFeeder().feed(key, item);
    }

    @Override
    public void flush() {
        getLocalFeeder().flush();
    }

    @Override
    public void close() throws Exception {
        getLocalFeeder().close();
        localFeederList.remove(getLocalFeeder());
        localFeeders.remove();
    }

    public void closeAllInputs() throws Exception {
        synchronized (this) {
            if (closed) { return; }
            closed = true;
        }

        for (ThreadLocalFeeder feeder : localFeederList) {
            feeder.close();
        }

        localFeederList.clear();
    }
}
