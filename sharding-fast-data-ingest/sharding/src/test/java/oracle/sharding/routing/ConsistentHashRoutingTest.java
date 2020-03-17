/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding.routing;

import junit.framework.TestCase;
import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Unit test for consistent hash routing table.
 */
public class ConsistentHashRoutingTest extends TestCase {
    static class TransparentRoutingKey implements RoutingKey {
        private final int directHash;

        private TransparentRoutingKey(int directHash) {
            this.directHash = directHash;
        }

        @Override
        public int hashCode() {
            return directHash;
        }

        @Override
        public int compareTo(RoutingKey o) {
            return Integer.compareUnsigned(directHash, o.hashCode());
        }
    }

    private final static AtomicInteger chunkId = new AtomicInteger(0);

    public static class TestChunk {
        public final String name;
        public final HashKeySet keys;
        public final long from;
        public final long to;
        public final int id;

        public TestChunk(String name, long from, long to) {
            this.keys = new HashKeySet(from, to);
            this.from = from;
            this.to   = to;
            this.id   = chunkId.incrementAndGet();
            this.name = name;
        }

        public TestChunk(long from, long to) {
            this.id   = chunkId.incrementAndGet();
            this.name = String.format("CHUNK_%d %d-%d", id, from, to);
            this.keys = new HashKeySet(from, to);
            this.from = from;
            this.to   = to;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Collection<TestChunk> findRealChunks(TestChunk [] chunkSet, long hashValue) {
        Collection<TestChunk> result = new HashSet<>();

        for (TestChunk chunk : chunkSet) {
            if (hashValue >= chunk.from && hashValue < chunk.to) {
                result.add(chunk);
            }
        }

        return result;
    }

    public static void compareToSet(Collection<TestChunk> _sampleChunks, Iterable<TestChunk> returnedChunks) {
        final Collection<TestChunk> sampleChunks = new HashSet<>(_sampleChunks);

        for (TestChunk chunk : returnedChunks) {
            boolean found = sampleChunks.remove(chunk);
            assertTrue("Sample set should be equal to returned chunk set (returned not in sample)",
                    found);
        }

        assertTrue("Sample set should be equal to returned chunk set (sample not in returned chunks)",
                sampleChunks.size() == 0);
    }

    public static void checkInSet(TestChunk [] chunkSet, RoutingTable<TestChunk> routingTable, int hashValue) {
        Iterable<TestChunk> chunksReturned = routingTable.find(new TransparentRoutingKey(hashValue));
        String allChunks = StreamSupport.stream(chunksReturned.spliterator(), false).map(x -> x.name)
                .collect(Collectors.joining(","));
/*
        System.out.printf("long %s : %s\n", Integer.toUnsignedString(hashValue), allChunks);
*/
        compareToSet(findRealChunks(chunkSet, Integer.toUnsignedLong(hashValue)), chunksReturned);
    }

    public static List<TestChunk> createRealisticChunks(int n) {
        List<TestChunk> chunkList = new ArrayList<>();
        long maxHash = 0x100000000L;

        for (int i = 0; i < n; ++i) {
            chunkList.add(new TestChunk("CHUNK_" + (i + 1), maxHash * (i) / n, maxHash * (i + 1) / n));
        }

        return chunkList;
    }

    public void testLikeReal() throws Exception {
        ConsistentHashRoutingTable<TestChunk> table = new ConsistentHashRoutingTable<>();

        List<TestChunk> chunkList = createRealisticChunks(100);
        TestChunk[] chunkSet = chunkList.toArray(new TestChunk[chunkList.size()]);

        RoutingTable.RoutingTableModifier<TestChunk> modifier = table.modifier();

        for (TestChunk chunk : chunkSet) {
            modifier.add(chunk, chunk.keys);
        }

        modifier.apply();

        int trials = 1000;

        for (int i = 0; i < trials; ++i) {
            checkInSet(chunkSet, table, (int) ((long) (i * 0x100000000L / trials)));
        }
    }

    public void testSimple() throws Exception {
        ConsistentHashRoutingTable<TestChunk> table = new ConsistentHashRoutingTable<>();

        TestChunk [] chunkSet = new TestChunk[] {
            new TestChunk(0, 200),
            new TestChunk(100, 300),
            new TestChunk(0, 300),
            new TestChunk(200, 400),
            new TestChunk(0, 300),
            new TestChunk(300, 500),
            new TestChunk(Integer.MAX_VALUE, ((long) Integer.MAX_VALUE + 1) * 2),
        };

        RoutingTable.RoutingTableModifier<TestChunk> modifier = table.modifier();

        for (TestChunk chunk : chunkSet) {
            modifier.add(chunk, chunk.keys);
        }

        modifier.apply();

        checkInSet(chunkSet, table, 0);
        checkInSet(chunkSet, table, 50);
        checkInSet(chunkSet, table, 100);
        checkInSet(chunkSet, table, 150);
        checkInSet(chunkSet, table, 200);
        checkInSet(chunkSet, table, 250);
        checkInSet(chunkSet, table, 300);
        checkInSet(chunkSet, table, 350);
        checkInSet(chunkSet, table, 400);
        checkInSet(chunkSet, table, 450);
        checkInSet(chunkSet, table, 500);
        checkInSet(chunkSet, table, 550);
        checkInSet(chunkSet, table, 0x80000000);
    }
}
