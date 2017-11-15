/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.SetOfKeys;

/**
 * Created by itaranov on 6/23/17.
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
}
