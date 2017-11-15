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
 * Created by itaranov on 4/19/17.
 */
public final class Shard {
    final Map<Integer, Chunk> chunks = new TreeMap<>();
    final private String name;
    private String connectionString;

    /* Modifiable by whoever whishes to */
    public volatile Object annotation;

    public Shard(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Collection<Chunk> getChunks()
    {
        return chunks.values();
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }
}
