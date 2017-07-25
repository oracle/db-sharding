package oracle.sharding.details;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingTable;
import oracle.sharding.SetOfKeys;
import oracle.sharding.SimpleKeyMetadata;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by somestuff on 6/23/17.
 */
public class SimpleRoutingTableWrapper extends OracleRoutingTable {
    private final RoutingTable<Chunk> subject;

    private SimpleRoutingTableWrapper(OracleShardingMetadata metadata, RoutingTable<Chunk> subject) {
        super(metadata);
        this.subject = subject;
    }

    public static<T> RoutingTable<T> createRoutingTable(SimpleKeyMetadata metadata) {
        switch (metadata.getType()) {
            case HASH: return new ConsistentHashRoutingTable<T>();
            default : throw new RuntimeException("Unexpected sharding type");
        }
    }

    public static SimpleRoutingTableWrapper create(OracleShardingMetadata globalMetadata, SimpleKeyMetadata metadata)
    {
        return new SimpleRoutingTableWrapper(globalMetadata, createRoutingTable(metadata));
    }

    @Override
    public boolean isEmpty() {
        return subject.isEmpty();
    }

    @Override
    public Collection<Chunk> lookup(RoutingKey key) {
        return subject.lookup(key);
    }

    @Override
    public Collection<Chunk> lookup(RoutingKey key, Collection<Chunk> result) {
        return subject.lookup(key, result);
    }

    @Override
    public Stream<Chunk> streamLookup(RoutingKey key) {
        return subject.streamLookup(key);
    }

    @Override
    public void atomicUpdate(Collection<Chunk> removeChunks, Collection<Chunk> updateChunks, Function<Chunk, SetOfKeys> getKeySet) {
        subject.atomicUpdate(removeChunks, updateChunks, getKeySet);
    }

    @Override
    public Collection<Chunk> values() {
        return subject.values();
    }
}
