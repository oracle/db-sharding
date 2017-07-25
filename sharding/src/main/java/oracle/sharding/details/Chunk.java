package oracle.sharding.details;

import oracle.sharding.SetOfKeys;

/**
 * Created by somestuff on 4/5/17.
 */
public final class Chunk implements Comparable<Chunk> {
    /* The chunk is uniquely identified by chunkUniqueId + shardName */
    private final int chunkUniqueId;
    private final Shard shard;
    private final int groupId;
    private final SetOfKeys keySet;

    private int status;
    private int chunkId; /* Optional informative field */

    /* Modifiable by whoever whishes to */
    public volatile Object annotation;

    @Override
    public int compareTo(Chunk o) {
        return Integer.compare(chunkUniqueId, o.chunkUniqueId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Chunk
                && chunkUniqueId == ((Chunk) obj).chunkUniqueId
                && shard == ((Chunk) obj).shard
                && keySet.equals(((Chunk) obj).getKeySet());
    }

    public Chunk(int chunkUniqueId, Shard shard, int groupId, SetOfKeys keySet) {
        this.chunkUniqueId = chunkUniqueId;
        this.shard = shard;
        this.groupId = groupId;
        this.keySet = keySet;
    }

    public Chunk updateKey(SetOfKeys newKeySet) {
        return new Chunk(chunkUniqueId, shard, groupId, newKeySet);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getChunkUniqueId() {
        return chunkUniqueId;
    }

    public Shard getShard() {
        return shard;
    }

    public SetOfKeys getKeySet() {
        return keySet;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getChunkId() {
        return chunkId;
    }

    public void setChunkId(int chunkId) {
        this.chunkId = chunkId;
    }
}
