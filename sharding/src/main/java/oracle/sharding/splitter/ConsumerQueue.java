package oracle.sharding.splitter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Created by somestuff on 4/6/17.
 */
public class ConsumerQueue<T> implements Consumer<T>, AutoCloseable {
    private final BlockingQueue<Object> queue;

    public ConsumerQueue(int queueSize) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public ConsumerQueue(int queueSize, Consumer<T> sink) {
        this(queueSize);
        createSink(sink);
    }

    public ConsumerQueue() {
        this(1024);
    }

    private static final Object END_MARKER = new Object();

    protected void sinkThreadRun(final Consumer<T> consumer) {
        try {
            Object x;

            while ((x = queue.take()) != END_MARKER) {
                consumer.accept((T) x);
            }

            /* put END_MARKER again in case there are any other
             * threads running the same task */
            queue.put(x);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (consumer instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) consumer).close();
                } catch (Exception e) {
                    /* ignore */;
                }
            }
        }
    }

    public Runnable createSink(Consumer<T> consumer)
    {
        return () -> sinkThreadRun(consumer);
    }

    @Override
    public void accept(T t) {
        try {
            queue.put(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() throws Exception {
        queue.put(END_MARKER);
    }

    public void closeIgnore() {
        try {
            queue.put(END_MARKER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
