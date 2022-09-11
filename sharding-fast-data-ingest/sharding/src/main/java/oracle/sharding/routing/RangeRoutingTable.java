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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Copy on write segment tree implementation
 */
public class RangeRoutingTable<T> implements RoutingTable<T> {
    private List<RangeSegment<T>> list = new ArrayList<>();

    public Iterable<T> find(RoutingKey key) {
        return () -> new ValueLookupIterator<T>(this.list, key);
    }

    @Override
    public Stream<T> streamLookup(RoutingKey key) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new ValueLookupIterator<T>(this.list, key), Spliterator.ORDERED), false);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableSet(list.stream().map(x -> x.value).collect(Collectors.toSet()));
    }

    @Override
    public RoutingTableModifier<T> modifier() {
        return null;
    }

    public static <T extends Comparable<T>> T max(T a, T b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    private static<T> int upperBound(List<RangeSegment<T>> list, RoutingKey x) {
        int low = 0;
        int high = list.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            boolean leqKey = list.get(mid).getLower().compareTo(x) <= 0;

            if (leqKey) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    private static class ValueLookupIterator<T> implements Iterator<T> {
        final List<RangeSegment<T>> list;
        int index;
        RoutingKey key;
        RangeSegment<T> cachedNext = null;

        private ValueLookupIterator(List<RangeSegment<T>> list, RoutingKey value) {
            this.list  = list;
            this.index = upperBound(list, value) - 1;
            this.key = value;
        }

        private void findNextItem() {
            while (index >= 0) {
                RangeSegment<T> item = list.get(index);

                /* Item found */
                if (item.contains(key)) {
                    cachedNext = item;
                    break;
                }

                /* Everything to the left (including item) is outside of the scope */
                if (item.max.compareTo(key) <= 0) {
                    index = -1;
                    break;
                }

                /* Look to the left */
                --index;
            }
        }

        public T next() {
            if (cachedNext == null) { findNextItem(); }
            RangeSegment<T> result = cachedNext;
            index--;
            cachedNext = null;
            return result.value;
        }

        public boolean hasNext() {
            if (cachedNext == null) { findNextItem(); }
            return index >= 0;
        }
    }

    private static class RangeSegment<T> extends RangeKeySet {
        final T value;
        RoutingKey max;

        public RangeSegment(RoutingKey lowerClosed, RoutingKey upperOpen, T value) {
            super(lowerClosed, upperOpen);
            this.value = value;
        }
    }

    private static <T> void buildMaxValues(List<RangeSegment<T>> list) {
        if (list.size() > 0) {
            RoutingKey max = list.get(0).getUpper();

            for (RangeSegment<T> item : list) {
                if (item.getUpper().compareTo(max) >= 0) {
                    max = item.getUpper();
                }

                /* Always update max which is null by default */
                item.max = max;
            }
        }
    }

    @SuppressWarnings("unchecked")
    static<MT extends Comparable> List<MT> mergeLists(
            List<MT> addSet, List<MT> currentSet, List<MT> result,
                Predicate<MT> preserve)
    {
        int i = 0, j = 0, n = currentSet.size(), m = addSet.size();

        while (i < n && j < m) {
            MT item = null /* not needed but whatever */, otherItem = addSet.get(j);

            while (i < n && (item = currentSet.get(i++)).compareTo(otherItem) <= 0) {
                if (preserve.test(item)) {
                    result.add(item);
                }
            }

            if (i < n) {
                otherItem = item;

                while (j < m && (item = addSet.get(j++)).compareTo(otherItem) <= 0) {
                    result.add(item);
                }
            }
        }

        while (i < n) {
            MT item = currentSet.get(i++);

            if (preserve.test(item)) {
                result.add(item);
            }
        }

        while (j < m) { result.add(addSet.get(j++)); }

        return result;
    }

    /**
     * Atomic modifier for the range routing table hash table
     */
    public class Modifier implements RoutingTableModifier<T> {
        final Set<T> removeSet = new HashSet<T>();
        ArrayList<RangeSegment<T>> addSet = new ArrayList<>();

        @Override
        public RoutingTableModifier<T> add(T value, SetOfKeys _keys) {
            RangeKeySet keys = (RangeKeySet) _keys;
            addSet.add(new RangeSegment<T>(keys.getLower(), keys.getUpper(), value));
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

            synchronized (RangeRoutingTable.this) {
                List<RangeSegment<T>> currentSet = RangeRoutingTable.this.list;
                List<RangeSegment<T>> result = new ArrayList<>(addSet.size() + currentSet.size());

                mergeLists(addSet, currentSet, result, (RangeSegment<T> item) -> !removeSet.contains(item.value));
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

            synchronized (RangeRoutingTable.this) {
                list = addSet;
            }

            addSet = new ArrayList<>();
            removeSet.clear();
        }
    }
}
