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
 * Represents a chunk in Sharding Metadata
 */
public final class Chunk implements Comparable<Chunk> {
    /* The chunk is uniquely identified by chunkUniqueId + shardName */
    private final int chunkUniqueId;
    private final Shard shard;
    private final int groupId;
    private final SetOfKeys keySet;

    private int status;
    private int priority; /* Golden Gate priority */

    private int chunkId; /* Optional informative field */

    /*
     * User annotation.
     * Modifiable by whoever whishes to
     **/
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

    public Chunk(int chunkId, int chunkUniqueId, Shard shard, int groupId, SetOfKeys keySet) {
    	this.chunkId = chunkId;
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

    public int getUniqueId() {
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isPrimary() { return status == 0 && priority == 0 && shard.isPrimary(); }

    public boolean isWritable() { return status == 0 && shard.isPrimary(); }
}
