package oracle.sharding.splitter;

import oracle.sharding.RoutingTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by somestuff on 6/21/17.
 */
public class ThreadBasedPartition<ItemT> extends PartitionEngine<ItemT> {
    private Map<Object, ConsumerQueue<List<ItemT>>> chunkQueues = new ConcurrentHashMap<>();
    private int queueSize = 1024;
    private final List<Thread> workingThreads = new ArrayList<>();

    public ThreadBasedPartition(RoutingTable routingTable) {
        super(new GeneralSplitter<ItemT>(routingTable));
        splitter.setSink(this::acceptItem);
    }

    private void acceptItem(Object container, List<ItemT> batch)
    {
        chunkQueues.computeIfAbsent(container, o -> new ConsumerQueue<>(queueSize,
                createSinkFunction.apply(container))).accept(batch);
    }

    public void createSink(Object chunk, Consumer<List<ItemT>> sink)
    {
        Thread thread = new Thread(chunkQueues
                .computeIfAbsent(chunk, o -> new ConsumerQueue<>(queueSize)).createSink(sink));
        thread.setDaemon(true);
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
        joinAll(waitTimeout);
        chunkQueues.values().forEach(ConsumerQueue::closeIgnore);
    }

    @Override
    public void close() throws Exception {
        interruptAll(); /* The best we can do */
        chunkQueues.values().forEach(ConsumerQueue::closeIgnore);
    }
}
