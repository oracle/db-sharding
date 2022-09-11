/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import oracle.sharding.details.Chunk;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.details.OracleKeyMetadata;
import oracle.sharding.details.Shard;
import oracle.sharding.routing.*;
import oracle.sql.CharacterSet;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Class, which reflects the model of Oracle Sharding metadata.
 */
public class OracleShardingMetadata implements RoutingMetadata {
    private CharacterSet oracleNLSCS;
    private CharacterSet oracleMultibyteNLSCS;

    private final SimpleKeyMetadata superShardingMetadata;
    private final SimpleKeyMetadata shardingMetadata;

    private final boolean isComposite;
    private final Map<String, Shard> shards = new ConcurrentHashMap<>();

    private interface SimpleKeySetFactory {
        SetOfKeys createKeySet(byte[] lowerClosed, byte[] upperOpen) throws SQLException;
    }

    private final SimpleKeySetFactory superShardingKeySetFactory;
    private final SimpleKeySetFactory shardingKeySetFactory;

    private OracleShardingMetadata(Builder builder) {
        if (builder.superShardingType != ShardBy.NONE) {
            superShardingMetadata =
                    new OracleKeyMetadata(builder.superShardingType,
                            builder.superColumns.stream().toArray(KeyColumn[]::new));
        } else {
            superShardingMetadata = null;
        }

        if (builder.shardingType != ShardBy.NONE) {
            shardingMetadata =
                    new OracleKeyMetadata(builder.shardingType,
                            builder.shardColumns.stream().toArray(KeyColumn[]::new));
        } else {
            shardingMetadata = null;
        }

        superShardingKeySetFactory = getKeySetFactory(builder.superShardingType);
        shardingKeySetFactory = getKeySetFactory(builder.shardingType);

        isComposite = superShardingMetadata != null && shardingMetadata != null;
    }

    /**
     * Return a key factory for the super level of sharding separately.
     * @return a key factory for the super level of sharding separately.
     */
    public SimpleKeyMetadata getSuperShardingMetadata() {
        return superShardingMetadata;
    }

    /**
     * Return a key factory for the main level of sharding separately.
     * @return a key factory for the main level of sharding separately.
     */
    public SimpleKeyMetadata getShardingMetadata() {
        return shardingMetadata;
    }

    /**
     * Return true if composite sharding is used
     */
    public boolean isComposite() {
        return this.isComposite;
    }

    public CharacterSet getOracleNLSCS() {
        return oracleNLSCS;
    }

    public void setOracleNLSCS(CharacterSet oracleNLSCS) {
        this.oracleNLSCS = oracleNLSCS;
    }

    public CharacterSet getOracleMultibyteNLSCS() {
        return oracleMultibyteNLSCS;
    }

    public void setOracleMultibyteNLSCS(CharacterSet oracleMultibyteNLSCS) {
        this.oracleMultibyteNLSCS = oracleMultibyteNLSCS;
    }

    private RoutingKey createShardingKey(Object ... a) throws SQLException {
        return getShardingMetadata().createKey(a);
    }

    private RoutingKey createSuperShardingKey(Object ... a) throws SQLException {
        return getSuperShardingMetadata().createKey(a);
    }

    private SimpleKeyMetadata getNonCompositeMetadata() {
        return shardingMetadata != null ? shardingMetadata : superShardingMetadata;
    }

    /**
     * Create a full (super+sharding if required in case of composite sharding) key
     *
     * @param a array of values
     * @param begin number of elements to skip from the beginning
     * @return a valid routing key
     * @throws SQLException if the objects provided are incompatible with the configuration
     */
    @Override
    public RoutingKey createKey(Object[] a, int begin) throws SQLException {
        return isComposite ? new CompositeRoutingKey(this,
                    superShardingMetadata.createKey(a, 0),
                    shardingMetadata.createKey(a, superShardingMetadata.getColumnCount()))
                : (getNonCompositeMetadata().createKey(a, begin));
    }

    /**
     * Create a full sharding key (super+sharding) from the array of values
     *
     * @param a array of values
     * @return a valid routing key
     * @throws SQLException if the objects provided are incompatible with the configuration
     */
    @Override
    public RoutingKey createKey(Object... a) throws SQLException {
        int superColumns = (superShardingMetadata != null ? superShardingMetadata.getColumnCount() : 0);
        int shardColumns = (shardingMetadata != null ? shardingMetadata.getColumnCount() : 0);

        if (a.length != (superColumns + shardColumns)) {
            throw new SQLException("Unexpected number of createKey values");
        }

        return createKey(a, 0);
    }

    public static<T> RoutingTable<T> createSimpleRoutingTable(SimpleKeyMetadata metadata) {
        switch (metadata.getType()) {
            case HASH: return new ConsistentHashRoutingTable<T>();
            default : throw new RuntimeException("Unexpected sharding type");
        }
    }

    public <T> RoutingTable<T> createRoutingTable() throws SQLException {
        if (isComposite) {
            return new CompositeRoutingTable<T>(this);
        } else {
            return createSimpleRoutingTable(getNonCompositeMetadata());
        }
    }

    private final WeakHashMap<ChunkTable, Void> createdTables = new WeakHashMap<>();

    /**
     * Update all created chunk tables with the new chunk information
     * NOTE: this method resets the chunk table completely.
     *
     * Thread-safe by being synchronized
     */
    public void updateChunkTables() {
        synchronized (this) {
            createdTables.keySet().forEach(this::updateChunkTable);
        }
    }

    /**
     * Update an untracked chunk table with the effective set of chunks
     * Thread-safe by being synchronized
     *
     * @param table to update
     */
    public void updateChunkTable(ChunkTable table) {
        RoutingTable.RoutingTableModifier<Chunk> modifier = table.modifier();

        synchronized (this) {
            for (Shard shard : shards.values()) {
                for (Chunk chunk : shard.getAllChunks()) {
                    modifier.add(chunk, chunk.getKeySet());
                }
            }
        }

        modifier.clearAndSet();
    }

    /**
     * Add a chunk table to be updated each time updateChunkTables() is called.
     * Thread-safe by being synchronized
     *
     * @param table to add
     * @return the same table
     */
    public ChunkTable addTrackedChunkTable(ChunkTable table) {

        synchronized (this) {
            createdTables.put(table, null);
        }

        return table;
    }

    /**
     * Create a routing table which maps routing keys to internal chunks
     * NOTE: Masks the SQL exception, which usually should not happen.
     *
     * Thread-safe by being synchronized
     *
     * @return an instance of a routing table
     */
    public ChunkTable createChunkTable()
    {
        try {
            return addTrackedChunkTable(ChunkTable.create(this));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create key set from oracle internal value representations.
     *
     * @param lowerClosed  lower bound key
     * @param upperOpen    upper bound key
     * @param groupLowerClosed group lower bound key
     * @param groupUpperOpen   group upper bound key
     * @return SetOfKeys instance
     * @throws SQLException if keys are not valid for the current implementation
     */
    public SetOfKeys createKeySet(byte[] lowerClosed, byte[] upperOpen, byte[] groupLowerClosed, byte[] groupUpperOpen)
            throws SQLException
    {
        SetOfKeys shardKeySet = shardingKeySetFactory.createKeySet(lowerClosed, upperOpen);

        if (isComposite) {
            SetOfKeys superKeySet = superShardingKeySetFactory.createKeySet(groupLowerClosed, groupUpperOpen);
            return new CompositeKeySet(superKeySet, shardKeySet);
        } else {
            return shardKeySet;
        }
    }

    /**
     * Find shard by name
     *
     * @param name shard name
     * @return Shard if known
     */
    public Shard getShard(String name) {
        return shards.get(name);
    }

    /**
     * Find shard by name or create it if not found.
     */
    public Shard getShardForUpdate(String name) {
        return shards.computeIfAbsent(name, Shard::new);
    }

    /**
     * Get the list of all known chunks.
     * @return a list of chunks
     */
    public Collection<Chunk> getAllChunks() {
        return shards.values().stream().flatMap(x -> x.getAllChunks().stream())
                .collect(Collectors.toList());
    }

    /**
     * Get the list of all known shards.
     * @return a list of shards
     */
    public Collection<Shard> getAllShards() {
        return new ArrayList<>(shards.values());
    }

    private static SimpleKeySetFactory nullKeySetFactory = (x, y) -> null;
    private static SimpleKeySetFactory hashKeySetFactory = HashKeySet::create;

    private static SimpleKeySetFactory getKeySetFactory(ShardBy type) {
        switch (type) {
            case HASH: return hashKeySetFactory;
            default  : return nullKeySetFactory;
        }
    }

    /**
     * Create a factory object.
     *
     * @param superShardingType supersharding type
     * @param shardingType sharding type
     * @return an instance of factory class.
     */
    public static Builder builder(ShardBy superShardingType, ShardBy shardingType) {
        return new Builder().setShardingType(shardingType).setSuperShardingType(superShardingType);
    }

    /**
     * Create a factory object.
     *
     * @return an instance of factory class.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory class for building an Oracle Sharding Metadata.
     */
    public static class Builder {
        private ShardBy shardingType = ShardBy.HASH;
        private ShardBy superShardingType = ShardBy.NONE;
        private final List<KeyColumn> superColumns = new ArrayList<>();
        private final List<KeyColumn> shardColumns = new ArrayList<>();

        public Builder setShardingType(ShardBy shardingType) {
            this.shardingType = shardingType;
            return this;
        }

        public Builder setSuperShardingType(ShardBy superShardingType) {
            this.superShardingType = superShardingType;
            return this;
        }

        public Builder addShardColumn(KeyColumn keyColumn) {
            shardColumns.add(keyColumn);
            return this;
        }

        public Builder addSuperColumn(KeyColumn keyColumn) {
            superColumns.add(keyColumn);
            return this;
        }

        public OracleShardingMetadata build() {
            return new OracleShardingMetadata(this);
        }
    }
}
