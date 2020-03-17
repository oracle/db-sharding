/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

import oracle.sharding.RoutingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * Partition engine which uses a thread per consumer
 */
public class ThreadBasedPartition<ItemT> extends PartitionEngine<ItemT> {
    private final Map<Object, ConsumerQueue<List<ItemT>>> chunkQueues = new ConcurrentHashMap<>();
    private int queueSize = 1024;
    private final List<Thread> workingThreads = new ArrayList<>();
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();

    public ThreadBasedPartition(RoutingTable routingTable) {
        super(new GeneralSplitter<ItemT>(routingTable));
        splitter.setSink(this::acceptItem);
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Create a queue for the chunk object.
     * Should be called once for each distinctive object.
     *
     * @param container chunk object to create a queue for
     * @return new queue
     */

    private ConsumerQueue<List<ItemT>> createContainerQueue(Object container)
    {
        ConsumerQueue<List<ItemT>> queue = new ConsumerQueue<>(queueSize);
        Consumer<List<ItemT>> sink = createSinkFunction.apply(container);
        Thread thread = threadFactory.newThread(queue.createSink(sink));
        thread.start();
        workingThreads.add(thread);
        return queue;
    }

    private void acceptItem(Object container, List<ItemT> batch)
    {
        chunkQueues.computeIfAbsent(container, o -> createContainerQueue(container)).accept(batch);
    }

    public void createSink(Object chunk, Consumer<List<ItemT>> sink)
    {
        ConsumerQueue<List<ItemT>> queue = chunkQueues
                .computeIfAbsent(chunk, o -> new ConsumerQueue<>(queueSize));
        Thread thread = threadFactory.newThread(queue.createSink(sink));
        thread.start();
        workingThreads.add(thread);
    }

    public void joinAll(long millis) {
        for (Thread t : workingThreads) {
            try {
                t.join(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        workingThreads.clear();
    }

    public void interruptAll() {
        for (Thread t : workingThreads) {
            t.interrupt();
        }
    }

    @Override
    public void waitAndClose(long waitTimeout) throws Exception {
        chunkQueues.values().forEach(ConsumerQueue::closeIgnore);
        joinAll(waitTimeout);
    }

    @Override
    public void close() throws Exception {
        chunkQueues.values().forEach(ConsumerQueue::closeIgnore);
    }
}
