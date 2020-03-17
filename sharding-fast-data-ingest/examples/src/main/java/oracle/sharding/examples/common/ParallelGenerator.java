/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * A helper to run similar tasks in parallel
 */
public class ParallelGenerator implements Runnable {
    private final Supplier<Runnable> supplier;
    private int numberOfThreads = 1;
    private ExecutorService executor;

    public ParallelGenerator(Supplier<Runnable> supplier) {
        this.supplier = supplier;
    }

    public ParallelGenerator times(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public ParallelGenerator execute(int numberOfThreads) {
        times(numberOfThreads);
        run();
        return this;
    }

    @Override
    public void run() {
        executor = Executors.newFixedThreadPool(numberOfThreads);
        IntStream.range(0, numberOfThreads).forEach((i) -> executor.submit(supplier.get()));
        executor.shutdown();
    }

    public void awaitTermination() throws InterruptedException {
        executor.awaitTermination(1024, TimeUnit.SECONDS);
    }
}
