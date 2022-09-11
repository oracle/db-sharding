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
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Task based partition engine
 */
public class TaskBasedPartition<ItemT> extends PartitionEngine<ItemT>
{
    private final Map<Object, ContainerData> containerInfo = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private int queueSize = 1024;
    private int maxBatchPerTask = 16;
    private OverflowPolicy<List<ItemT>> overflowPolicy = OverflowPolicy.WAIT_FOR_SINK;

    public TaskBasedPartition(RoutingTable routingTable, ExecutorService executor) {
        super(new GeneralSplitter<ItemT>(routingTable));
        this.executor = executor;
        splitter.setSink(this::acceptItem);
    }

    private void acceptItem(Object container, List<ItemT> batch)
    {
        ContainerData containerData = containerInfo.computeIfAbsent(container, ContainerData::new);
        containerData.createJob(batch);
    }

    public void createSink(Object chunk, Consumer<List<ItemT>> sink) {
        containerInfo.computeIfAbsent(chunk, ContainerData::new).sinkList.offer(sink);
    }

    @Override
    public void close() throws Exception {
        splitter.closeAllInputs();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    private class ContainerData {
        private final BlockingQueue<List<ItemT>> batchQueue = new ArrayBlockingQueue<>(queueSize);
        private ConcurrentLinkedQueue<Consumer<List<ItemT>>> sinkList = new ConcurrentLinkedQueue<>();
        private Object container;

        public ContainerData(Object container) {
            this.container = container;
            sinkList.offer(createSinkFunction.apply(container));
        }

        private void submitCheckQueue()
        {
            if (!batchQueue.isEmpty()) {
                Consumer<List<ItemT>> worker = sinkList.poll();

                if (worker != null) {
                    createJobInternal(worker, null);
                } else {
                    executor.submit(this::submitCheckQueue);
                }
            }
        }

        private void createJobInternal(
                final Consumer<List<ItemT>> worker,
                final List<ItemT> batch)
        {
            executor.submit(() -> {
                List<ItemT> localBatch = batch;
                int batchPerTask = TaskBasedPartition.this.maxBatchPerTask;

                do {
                    if (localBatch != null) {
                        worker.accept(localBatch);
                    }

                    if (--batchPerTask > 0) {
                        localBatch = batchQueue.poll();
                    } else {
                        localBatch = null;
                    }
                } while (localBatch != null);

                if (worker instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) worker).close();
                    } catch (Exception e) {
                        /* ignore */;
                    }
                }
                sinkList.offer(worker);

                submitCheckQueue();
            });
        }

        void createJob(List<ItemT> batch) {
            Consumer<List<ItemT>> worker = sinkList.poll();

            if (worker == null) {
                if (batchQueue.offer(batch)) {
                    /* Make sure that there is someone to take care of a new batch */
                    batch = null;

                    if ((worker = sinkList.poll()) == null) {
                        return;
                    }
                } else {
                    Object whatToDo = overflowPolicy.apply(container, batch);

                    if (whatToDo == OverflowPolicy.CREATE_SINK) {
                        if (createSinkFunction != null) {
                            worker = createSinkFunction.apply(container);
                        } else {
                            /* TODO : throw unexpected result */
                            return;
                        }
                    } else if (whatToDo == OverflowPolicy.WAIT_FOR_SINK) {
                        try {
                            batchQueue.put(batch);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else if (whatToDo == null) {
                        /* Drop input */
                        return;
                    } else {
                        /* TODO : throw unexpected result */
                        return;
                    }
                }
            }

            createJobInternal(worker, batch);
        }
    }

    public void waitAndClose(long waitTimeout) throws Exception
    {
        splitter.closeAllInputs();
        executor.shutdown();
        executor.awaitTermination(waitTimeout, TimeUnit.MILLISECONDS);
    }
}
