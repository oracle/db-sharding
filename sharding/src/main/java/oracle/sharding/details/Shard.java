package oracle.sharding.details;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by somestuff on 4/19/17.
 */
public final class Shard {
    final Map<Integer, Chunk> chunks = new TreeMap<>();
    final private String name;

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
}
