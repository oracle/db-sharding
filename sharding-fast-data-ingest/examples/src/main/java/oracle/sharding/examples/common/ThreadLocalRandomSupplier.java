/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.common;

import java.util.function.LongSupplier;

/**
 * Fast thread-unsafe supplier for long numbers
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
