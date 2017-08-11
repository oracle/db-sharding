package oracle.sharding.examples;

import java.util.function.LongSupplier;

/**
 * Created by somestuff on 8/10/17.
 */
public class ThreadLocalRandomSupplier implements LongSupplier {
    private long x = System.currentTimeMillis();

    @Override
    public long getAsLong() {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }
}
