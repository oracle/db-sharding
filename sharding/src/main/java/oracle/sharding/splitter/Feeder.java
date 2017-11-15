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
 * Created by itaranov on 6/9/17.
 */
public interface Feeder<ItemT> extends AutoCloseable {
    void feed(ItemT item);
    void feed(RoutingKey key, ItemT item);
    void flush();
}
