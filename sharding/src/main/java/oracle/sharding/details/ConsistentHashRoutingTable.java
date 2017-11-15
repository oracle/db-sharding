/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;
import oracle.sharding.SetOfKeys;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by itaranov on 4/1/17.
 */
public class ConsistentHashRoutingTable<T> implements RoutingTable<T> {
    private volatile BulkSortedList<T> hashRanges = new BulkSortedList<>();

    @Override
    public Collection<T> values() {
        return hashRanges.values();
    }

    @Override
    public boolean isEmpty() {
        return hashRanges.list.isEmpty();
    }

    @Override
    public void atomicUpdate(Collection<T> removeChunks, Collection<T> updateChunks, Function<T, SetOfKeys> getKeySet) {
        update(makeSureHashSet(removeChunks), makeSureHashSet(updateChunks), getKeySet);
    }

    private static<T> HashSet<T> makeSureHashSet(Collection<T> x) {
        return (x instanceof HashSet) ? (HashSet<T>) x : new HashSet<T>(x);
    }

    private void update(Set<T> removeChunks, Set<T> addChunks, Function<T, SetOfKeys> getKeySet)
    {
        removeChunks.removeAll(addChunks);

        synchronized (this) {
            BulkSortedList<T> newRanges = new BulkSortedList<T>(hashRanges);

            newRanges.removeAll(removeChunks, getKeySet);
            newRanges.addAll(addChunks, getKeySet);
            newRanges.cleanup();

            hashRanges = newRanges;
        }
    }

    private static <T> int upperBound(List<HashRoutingNode<T>> l, long hashValue) {
        int low = 0;
        int high = l.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            boolean lessThenKey = l.get(mid).compareLess(hashValue);

            if (lessThenKey) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    public class BoundIterator extends Spliterators.AbstractSpliterator<HashRoutingNode<T>> {
        private final List<HashRoutingNode<T>> list;
        private final int idx;

        protected BoundIterator(List<HashRoutingNode<T>> list, int idx) {
            super(idx < list.size() ? list.get(idx).chunks.size() : 0, 0);

            this.list = list;
            this.idx = idx;
        }

        @Override
        public boolean tryAdvance(Consumer<? super HashRoutingNode<T>> action) {
            if (idx < list.size()) {
                action.accept(list.get(idx));
                return true;
            } else {
                return false;
            }
        }
    }

    public BoundIterator boundIterator(RoutingKey key)
    {
        List<HashRoutingNode<T>> list = hashRanges.list;
        long hash = Integer.toUnsignedLong(key.hashCode());
        int index = upperBound(list, hash) - 1;
        assert list.get(index).contains(hash);
        return new BoundIterator(list, index >= 0 ? index : list.size());
    }

    @Override
    public Stream<T> streamLookup(RoutingKey key) {
        return StreamSupport.stream(boundIterator(key), false).flatMap(x -> x.chunks.stream());
    }

    private static final class HashRoutingNode<T> extends HashKeySet {
        final Set<T> chunks = new CopyOnWriteArraySet();

        HashRoutingNode(long lowerClosed, long upperOpen) {
            super(lowerClosed, upperOpen);
        }

        public HashRoutingNode(HashKeySet key, T chunk) {
            this(key.lowerClosed, key.upperOpen);
            chunks.add(chunk);
        }
    }

    private static class BulkSortedList<T> {
        private final List<HashRoutingNode<T>> list;

        public BulkSortedList() {
            this.list = Collections.EMPTY_LIST;
        }

        public BulkSortedList(BulkSortedList<T> copyFrom) {
            this.list = new ArrayList<>(copyFrom.list);
        }

        void removeAll(Collection<T> chunks, Function<T, SetOfKeys> getKeySet) {
            for (T chunk : chunks) {
                int position = Collections.binarySearch(list, (HashKeySet) getKeySet.apply(chunk));

                if (position >= 0) {
                    list.get(position).chunks.remove(chunk);
                }
            }
        }

        void addAll(Collection<T> chunks, Function<T, SetOfKeys> getKeySet) {
            for (T chunk : chunks) {
                int position = Collections.binarySearch(list, (HashKeySet) getKeySet.apply(chunk));

                if (position >= 0) {
                    list.get(position).chunks.add(chunk);
                } else {
                    list.add(-(position + 1), new HashRoutingNode<T>((HashKeySet) getKeySet.apply(chunk), chunk));
                }
            }
        }

        void cleanup() {
            list.removeIf(x -> x.chunks.isEmpty());
        }

        public Collection<T> values() {
            return list.stream().flatMap(x -> x.chunks.stream()).collect(Collectors.toSet());
        }
    }

}
