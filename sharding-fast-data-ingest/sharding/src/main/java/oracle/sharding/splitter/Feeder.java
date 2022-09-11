/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

import oracle.sharding.RoutingKey;

/**
 * Interface for a object feeder
 */
public interface Feeder<ItemT> extends AutoCloseable {
    /**
     * Feed the object to splitter if the
     * getKey function is defined at splitter
     *
     * @param item item to feed to consumers
     */
    void feed(ItemT item);

    /**
     * Feed the object to splitter with explicitly specifying the key
     *
     * @param key  routing key, which corresponds to the item
     * @param item item to feed to consumers
     */
    void feed(RoutingKey key, ItemT item);

    /**
     * Send accumulated items (if any) to consumers
     */
    void flush();
}
