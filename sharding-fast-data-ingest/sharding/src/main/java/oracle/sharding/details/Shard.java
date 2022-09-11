/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a shard
 */
public final class Shard {
    final Map<Integer, Chunk> chunks = new TreeMap<>();
    final private String name;
    private String connectionString;
    private boolean primary;

    /*
     * User annotation.
     * Modifiable by whoever whishes to
     **/
    public volatile Object annotation;

    public Shard(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<Chunk> getAllChunks()
    {
        return chunks.values();
    }

    public void addChunk(Chunk chunk) {
        chunks.put(chunk.getUniqueId(), chunk);
    }

    public Chunk getChunk(int uniqueId) {
        return chunks.get(uniqueId);
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void clearChunks() {
        chunks.clear();
    }
}
