/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding;

/**
 * RoutingKey is an empty logical interface to separate the concept of
 * key form any type.
 *
 * Technically, anything can be a key. However, making a key specific to this interface we make sure,
 * that implementation is aware, that key should possess special properties.
 *
 * We want to prevent errors like this:
 *
 *   ...
 *   RoutingTable x;
 *   ...
 *   x.lookup(5);
 *   ...
 *
 *  In this case, 5 is unlikely the key we want to pass, instead, we want to make sure, it's what we want:
 *
 *   ...
 *   RoutingTable x;
 *   ...
 *   x.lookup(metadata.createKey(5));
 *   ...
 *
 *  In fact lookup() is meant to be an "internal method", routing table implementations are expected to
 *  have their own .
 *  ChunkTable implements a separate method
 *  getAnyChunk(), which can be used instead:
 *
 */
public interface RoutingKey extends Comparable<RoutingKey> {
}
