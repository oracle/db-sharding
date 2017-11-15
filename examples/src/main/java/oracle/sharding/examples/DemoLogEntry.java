/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples;

import java.util.Random;
import java.util.function.LongSupplier;

/**
 * Created by itaranov on 7/28/17.
 */
public class DemoLogEntry {
    private String customerId;
    private String ipAddress;
    private long hits;

    public String getCustomerId() {
        return customerId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getHits() {
        return hits;
    }

    public DemoLogEntry(String customerId, String ipAddress, long hits) {
        this.customerId = customerId;
        this.ipAddress = ipAddress;
        this.hits = hits;
    }

    public static final Random rand = new Random();

    public static DemoLogEntry generate() {
        String bogus = String.format("%04x:%04x:%04x:%04x",
                rand.nextInt(65000),
                rand.nextInt(65000),
                rand.nextInt(65000),
                rand.nextInt(65000));

        return new DemoLogEntry( String.valueOf(rand.nextInt(1024)), bogus, rand.nextInt(10240));
    }

    public static DemoLogEntry generate(LongSupplier rnd) {
        return new DemoLogEntry(
                String.valueOf(rnd.getAsLong() & 0xfff),
                String.format("%04x:%04x:%04x:%04x",
                        rnd.getAsLong() & 0xffff,
                        rnd.getAsLong() & 0xffff,
                        rnd.getAsLong() & 0xffff,
                        rnd.getAsLong() & 0xffff),
                rnd.getAsLong() & 0xfffff);
    }

    public static String generateString(LongSupplier rnd) {
        return "" + (rnd.getAsLong() & 0xffff) + ","
                + Long.toUnsignedString(rnd.getAsLong()) + ","
                + Long.toUnsignedString(rnd.getAsLong());

/*
        return String.format("%d,%04x:%04x:%04x:%04x,%d",
                rand.nextInt(1024),
                rand.nextInt(65000),
                rand.nextInt(65000),
                rand.nextInt(65000),
                rand.nextInt(65000),
                rand.nextInt(10240));
*/
    }

    @Override
    public String toString() {
        return customerId + "," + ipAddress + "," + hits;
    }
}
