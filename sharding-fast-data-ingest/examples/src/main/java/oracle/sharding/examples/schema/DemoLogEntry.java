/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.schema;

import java.util.Random;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A mock class representing a log entry.
 * We use this in examples.
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

    public static String getCreateTable() {
        String [] tableColumns = {
                "CUST_ID VARCHAR2(128) NOT NULL",
                "IP_ADDR VARCHAR2(128) NOT NULL",
                "HITS INTEGER NOT NULL" };

        return "CREATE SHARDED TABLE LOG (" + Stream.of(tableColumns).collect(Collectors.joining(",")) + ", " +
                " CONSTRAINT LOG_PK PRIMARY KEY (CUST_ID, IP_ADDR) )"
                + "PARTITION BY CONSISTENT HASH (CUST_ID) PARTITIONS AUTO TABLESPACE SET DEFLTS";
    }

    public static String generateString(LongSupplier rnd) {
        return "" + (rnd.getAsLong() & 0xffff) + ","
                + Long.toUnsignedString(rnd.getAsLong()) + ","
                + Long.toUnsignedString(rnd.getAsLong());
    }

    @Override
    public String toString() {
        return customerId + "," + ipAddress + "," + hits;
    }
}
