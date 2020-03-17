/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RoutingTable class represents an multimap that virtually maps routing keys to values.
 * Normally, value would be a chunk, but it can be anything.
 *
 * The routing table implementation should be thread-safe.
 */
public interface RoutingTable<T> {
    /**
     * Find the routing key in the routing table and return all objects,
     * which correspond to that routing key.
     *
     * NOTE: there can be many items, which correspond to one single key
     * (for example, in case of range being partially split)
     *
     * @param result Collection to put the result in. If null, some List
     *               implementation is created.
     * @param key    Set of routing keys parts to lookup
     * @return Collection either created, or passed in as result parameter.
     */
    default Collection<T> lookup(RoutingKey key, Collection<T> result) {
        if (result == null) { return lookup(key); }
        streamLookup(key).forEach(result::add);
        return result;
    }

    default Collection<T> lookup(RoutingKey key) {
        return streamLookup(key).collect(Collectors.toSet());
    }

    /**
     * Find the routing key in the routing table and return all objects,
     * which correspond to that routing key.
     *
     * This method differs from lookup only in return type.
     * It returns iterable, making it possible to optimize implementation
     * by avoiding a redundant collection creation.
     *
     * @param key key to find
     * @return "finder" object, which returns a new iterator each time to
     *          traverse all values for the given key
     */
    default Iterable<T> find(RoutingKey key) {
        return lookup(key);
    }

    Stream<T> streamLookup(RoutingKey key);

    boolean isEmpty();

    /**
     * Return all the values known to the object.
     *
     * @return Unmodifiable collection of values.
     */
    Collection<T> values();

    /**
     * Interface for atomic updating of a routing table.
     *
     * To allow for more efficient implementations, all
     * routing table updates are expected to be bulk updates.
     **/
    interface RoutingTableModifier<T> {
        /**
         * Add key set
         *
         * @param value value
         * @param keys set of keys
         */
        RoutingTableModifier<T> add(T value, SetOfKeys keys);

        /**
         * Remove value (all appearances)
         *
         * @param value value
         */
        RoutingTableModifier<T> remove(T value);

        default void removeAll(Collection<? extends T> values) {
            for (T value : values) { remove(value); }
        }

        /**
         * Apply the modification.
         * Modifier object should be discarded after apply.
         */
        void apply();

        /**
         * Clear the routing table and set the newly added values.
         * Modifier object should be discarded after apply.
         */
        void clearAndSet();
    }

    /**
     * Request an bulk atomic modifier for the routing table structure.
     *
     * @return a new modifier object
     */
    RoutingTableModifier<T> modifier();
}
