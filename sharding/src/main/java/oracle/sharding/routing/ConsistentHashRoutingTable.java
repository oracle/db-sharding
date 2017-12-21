/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;
import oracle.sharding.SetOfKeys;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static oracle.sharding.routing.RangeRoutingTable.mergeLists;

/**
 * Routing table implementation for consistent hash.
 */
public class ConsistentHashRoutingTable<T> implements RoutingTable<T> {
    /* Volatile in Java terms not needed. We are o.k. if the list is cached locally,
     * we copy it for every operation anyway */
    private List<HashSegment<T>> list = new ArrayList<>();

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableSet(list.stream().map(x -> x.value).collect(Collectors.toSet()));
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public RoutingTableModifier<T> modifier() {
        return new Modifier();
    }

    /**
     * Find the upper bound for the given value in the list,
     * i.e. the first segment, which lower bound is greater then given value.
     *
     * This would means that all the segments to the right (and starting with
     * the given element) do not contain the given value
     *
     * @param list list to fix
     * @param hashValue value to look for
     * @return index of the element, starting from which segments start from a greater
     *      value then the given hash value
     */
    private static <T> int upperBound(List<HashSegment<T>> list, long hashValue) {
        int low = 0;
        int high = list.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            boolean leqKey = list.get(mid).lowerClosed <= hashValue;

            if (leqKey) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    private static class ValueLookupIterator<T> implements Iterator<T> {
        final List<HashSegment<T>> list;
        final long key;
        int index;

        /* The requirement is not to initiate the
         * search for the next element until we a really asked for it,
         * We expect in most cases there will be just one element needed */
        HashSegment<T> cachedNext = null;

        private ValueLookupIterator(List<HashSegment<T>> list, long value) {
            this.list  = list;
            this.index = upperBound(list, value) - 1;
            this.key = value;
        }

        private void findNextItem() {
            while (index >= 0) {
                HashSegment<T> item = list.get(index);

                /* Item found */
                if (item.contains(key)) {
                    cachedNext = item;
                    break;
                }

                /* Everything to the left (including item) is outside of the scope */
                if (item.max <= key) {
                    index = -1;
                    break;
                }

                /* Look to the left */
                --index;
            }
        }

        public T next() {
            if (cachedNext == null) { findNextItem(); }
            HashSegment<T> result = cachedNext;
            index--;
            cachedNext = null;
            return result.value;
        }

        public boolean hasNext() {
            if (cachedNext == null) { findNextItem(); }
            return index >= 0;
        }
    }

    public Iterable<T> find(RoutingKey key) {
        final long hashCode = Integer.toUnsignedLong(key.hashCode());
        return () -> new ValueLookupIterator<T>(this.list, hashCode);
    }

    @Override
    public Stream<T> streamLookup(RoutingKey key) {
        final long hashCode = Integer.toUnsignedLong(key.hashCode());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new ValueLookupIterator<T>(this.list, hashCode), Spliterator.ORDERED), false);
    }

    private static final class HashSegment<T> extends HashKeySet {
        final T value;

        /* Maximum high bound value, which we can encounter to the left of this item (inclusive) */
        long max;

        public HashSegment(long a, long b, T value) {
            super(a, b);
            this.value = value;
        }
    }

    private static <T> void buildMaxValues(List<HashSegment<T>> list) {
        if (list.size() > 0) {
            long max = list.get(0).upperOpen;

            for (HashSegment<T> item : list) {
                if (item.upperOpen >= max) {
                    max = item.upperOpen;
                }

                /* Always update max which is zero by default */
                item.max = max;
            }
        }
    }

    /**
     * Atomic modifier for the consistent hash table
     */
    public class Modifier implements RoutingTableModifier<T> {
        final Set<T> removeSet = new HashSet<T>();
        ArrayList<HashSegment<T>> addSet = new ArrayList<>();

        @Override
        public RoutingTableModifier<T> add(T value, SetOfKeys keys) {
            HashKeySet hashKeys = (HashKeySet) keys;
            addSet.add(new HashSegment<>(hashKeys.lowerClosed, hashKeys.upperOpen, value));
            return this;
        }

        @Override
        public RoutingTableModifier<T> remove(T value) {
            removeSet.add(value);
            return this;
        }

        @Override
        public void apply() {
            Collections.sort(addSet);

            synchronized (ConsistentHashRoutingTable.this) {
                List<HashSegment<T>> currentSet = ConsistentHashRoutingTable.this.list;
                List<HashSegment<T>> result = new ArrayList<>(addSet.size() + currentSet.size());

                mergeLists(addSet, currentSet, result, (item) -> !removeSet.contains(item.value));
                buildMaxValues(result);

                list = result;
            }

            addSet = new ArrayList<>();
            removeSet.clear();
        }

        @Override
        public void clearAndSet() {
            Collections.sort(addSet);
            buildMaxValues(addSet);

            synchronized (ConsistentHashRoutingTable.this) {
                list = addSet;
            }

            addSet = new ArrayList<>();
            removeSet.clear();
        }
    }

}
