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
import java.util.stream.Stream;

/**
 * List routing table which is implemented with
 * copy on write multimap
 */
public class ListRoutingTable<T> implements RoutingTable<T> {
    private Map<RoutingKey, List<T>> impl = new HashMap<>();

    @Override
    public Stream<T> streamLookup(RoutingKey key) {
        return impl.get(key).stream();
    }

    @Override
    public Iterable<T> find(RoutingKey key) {
        return Collections.unmodifiableCollection(impl.get(key));
    }

    @Override
    public Collection<T> lookup(RoutingKey key) {
        return Collections.unmodifiableCollection(impl.get(key));
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public Collection<T> values() {
        return null;
    }

    @Override
    public RoutingTableModifier<T> modifier() {
        return new Modifier();
    }

    /**
     * Atomic modifier for the list table
     */
    private class Modifier implements RoutingTableModifier<T> {
        final Set<T> removeSet = new HashSet<T>();
        private Map<RoutingKey, List<T>> addSet = new HashMap<>();

        @Override
        public RoutingTableModifier<T> add(T value, SetOfKeys keys) {
            for (RoutingKey k: ((ListKeySet) keys).getKeys()) {
                addSet.computeIfAbsent(k, (__) -> new ArrayList<>()).add(value);
            }

            return this;
        }

        @Override
        public RoutingTableModifier<T> remove(T value) {
            removeSet.add(value);
            return this;
        }

        @Override
        public void apply() {
            synchronized (ListRoutingTable.this) {
                Map<RoutingKey, List<T>> newMap = new HashMap<>();

                for (RoutingKey k: impl.keySet()) {
                    List<T> list = newMap.computeIfAbsent(k, (__) -> new ArrayList<>());

                    for (T v : impl.get(k)) {
                        if (!removeSet.contains(v)) {
                            list.add(v);
                        }
                    }
                }

                for (RoutingKey k: addSet.keySet()) {
                    newMap.computeIfAbsent(k, (__) -> new ArrayList<>()).addAll(addSet.get(k));
                }

                impl = newMap;
            }
        }

        @Override
        public void clearAndSet() {
            synchronized (ListRoutingTable.this) {
                impl = addSet;
            }
        }
    }
}
