/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.SetOfKeys;

/**
 * Set of keys for composite sharding
 */
public class CompositeKeySet extends SetOfKeys {
    private final SetOfKeys groupKeySet;
    private final SetOfKeys shardKeySet;

    public CompositeKeySet(SetOfKeys groupKeySet, SetOfKeys shardKeySet) {
        this.groupKeySet = groupKeySet;
        this.shardKeySet = shardKeySet;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompositeKeySet
                && ((CompositeKeySet) obj).groupKeySet.equals(this.groupKeySet)
                && ((CompositeKeySet) obj).shardKeySet.equals(this.shardKeySet);
    }

    public SetOfKeys getGroupKeySet() {
        return groupKeySet;
    }

    public SetOfKeys getShardKeySet() {
        return shardKeySet;
    }

    @Override
    public boolean contains(RoutingKey routingKey) {
        return routingKey instanceof CompositeRoutingKey
            && groupKeySet.contains(((CompositeRoutingKey) routingKey).getSuperShardingKey())
            && shardKeySet.contains(((CompositeRoutingKey) routingKey).getShardingKey());
    }
}
