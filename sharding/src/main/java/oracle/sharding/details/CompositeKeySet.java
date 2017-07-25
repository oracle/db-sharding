package oracle.sharding.details;

import oracle.sharding.SetOfKeys;

/**
 * Created by somestuff on 6/23/17.
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
