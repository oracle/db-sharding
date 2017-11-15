/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RoutingTable class represents an object that virtually maps routing keys to values,
 * representing parts, containing that values. Normally, value would be a chunk.
 */
public interface RoutingTable<T> {
    /**
     * Find the createKey (complex) in the routing table and return all objects,
     * which correspond to that createKey.
     *
     * NOTE: there can be many items, which correspond to one single createKey
     * (for example, in case of range being partially split)
     *
     * @param result Collection to put the result in. If null, some List implementation is created.
     * @param key    Set of createKey parts to lookup
     * @return Collection either created, or passed in as result parameter.
     */
    default Collection<T> lookup(RoutingKey key, Collection<T> result) {
        streamLookup(key).forEach(result::add);
        return result;
    }

    default Collection<T> lookup(RoutingKey key) {
        return streamLookup(key).collect(Collectors.toSet());
    }

    Stream<T> streamLookup(RoutingKey key);

    /**
     * Atomically update a set of chunks.
     *
     * First, remove all chunks from remove list.
     * Second, update or add all the chunks from update list.
     * Sets can intersect, but remove is applied before update.
     *
     * @param removeChunks Set of chunks to remove.
     * @param updateChunks Set of chunks to update/add.
     * @param getKeySet Mapping between chunks and createKey sets.
     */
    void atomicUpdate(Collection<T> removeChunks, Collection<T> updateChunks, Function<T, SetOfKeys> getKeySet);

    boolean isEmpty();

    /**
     * Return all the chunks (values) known to the object.
     *
     * @return Unmodifiable collection of values.
     */
    Collection<T> values();

    default void update(T chunk, SetOfKeys keys) {
        throw new UnsupportedOperationException("Method is not implemented for this object");
    }

    default void remove(T chunk) {
        throw new UnsupportedOperationException("Method is not implemented for this object");
    }
}
