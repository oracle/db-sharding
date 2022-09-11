/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Queue for a multithreaded consumer
 */
public class ConsumerQueue<T> implements Consumer<T>, AutoCloseable {
    private final BlockingQueue<Object> queue;

    public ConsumerQueue(int queueSize) {
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public ConsumerQueue() {
        this(1024);
    }

    private static final Object END_MARKER = new Object();

    private class ConsumerQueueSpliterator extends Spliterators.AbstractSpliterator<T>
    {
        protected ConsumerQueueSpliterator() {
            super(Long.MAX_VALUE, 0);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                Object x;

                if ((x = queue.take()) != END_MARKER) {
                    action.accept((T) x);
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public Stream<T> stream() {
        return StreamSupport.stream(new ConsumerQueueSpliterator(), false);
    }

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
