package oracle.sharding.examples;

import java.util.Random;

/**
 * Created by somestuff on 7/28/17.
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
        return new DemoLogEntry(
                String.valueOf(rand.nextInt(1024)),
                String.valueOf("" + rand.nextInt(250) + "."
                    + rand.nextInt(250) + "."
                    + rand.nextInt(250) + "."
                    + rand.nextInt(250)),
                rand.nextInt(10240));
    };

    @Override
    public String toString() {
        return customerId + "," + ipAddress + "," + hits;
    }
}
