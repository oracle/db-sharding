/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.*;
import oracle.sql.CharacterSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by itaranov on 4/2/17.
 */
public class OracleShardingMetadata extends RoutingMetadata {
    private final int tableFamilyId;
    private CharacterSet oracleNLSCS;
    private CharacterSet oracleMultibyteNLSCS;

    private final SimpleKeyMetadata superShardingMetadata;
    private final SimpleKeyMetadata shardingMetadata;

    private final boolean isComposite;
    private final Map<String, Shard> shards = new ConcurrentHashMap<>();

    public interface SimpleKeySetFactory {
        SetOfKeys createKeySet(byte[] lowerClosed, byte[] upperOpen) throws SQLException;
    }

    private final SimpleKeySetFactory superShardingKeySetFactory;
    private final SimpleKeySetFactory shardingKeySetFactory;

    private OracleShardingMetadata(OracleMetadataBuilder builder) {
        this.tableFamilyId = builder.tableFamilyId;

        if (builder.superShardingType != ShardBy.NONE) {
            superShardingMetadata =
                    new SimpleKeyMetadata(builder.superShardingType,
                            builder.superColumns.stream().toArray(KeyColumn[]::new));
        } else {
            superShardingMetadata = null;
        }

        if (builder.shardingType != ShardBy.NONE) {
            shardingMetadata =
                    new SimpleKeyMetadata(builder.shardingType,
                            builder.shardColumns.stream().toArray(KeyColumn[]::new));
        } else {
            shardingMetadata = null;
        }

        superShardingKeySetFactory = getKeySetFactory(builder.superShardingType);
        shardingKeySetFactory = getKeySetFactory(builder.shardingType);

        isComposite = superShardingMetadata != null && shardingMetadata != null;
    }

    public int getTableFamilyId() {
        return tableFamilyId;
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

    public SimpleKeyMetadata getSuperShardingMetadata() {
        return superShardingMetadata;
    }

    public SimpleKeyMetadata getShardingMetadata() {
        return shardingMetadata;
    }

    public boolean isComposite() {
        return this.isComposite;
    }

    public SimpleRoutingKey createShardingKey(Object ... a) throws SQLException {
        return getShardingMetadata().createKey(a);
    }

    public SimpleRoutingKey createSuperShardingKey(Object ... a) throws SQLException {
        return getSuperShardingMetadata().createKey(a);
    }

    private SimpleKeyMetadata getNonCompositeMetadata() {
        return shardingMetadata != null ? shardingMetadata : superShardingMetadata;
    }

    @Override
    public RoutingKey createKey(Object[] a, int begin) throws SQLException {
        return isComposite ? new CompositeRoutingKey(this,
                    superShardingMetadata.createKey(a, 0),
                    shardingMetadata.createKey(a, superShardingMetadata.getColumnCount()))
                : (getNonCompositeMetadata().createKey(a, begin));
    }

    @Override
    public RoutingKey createKey(Object... a) throws SQLException {
        if (a.length != ((superShardingMetadata != null ? superShardingMetadata.getColumnCount() : 0)
                + (shardingMetadata != null ? shardingMetadata.getColumnCount() : 0)))
        {
            throw new SQLException("Unexpected number of createKey values");
        }

        return createKey(a, 0);
    }

    public OracleRoutingTable createRoutingTable() throws SQLException
    {
        if (isComposite) {
            return new CompositeRoutingTable(this);
        } else {
            return SimpleRoutingTableWrapper.create(this, getNonCompositeMetadata());
        }
    }

    public SetOfKeys createKeySet(byte[] lowerClosed, byte[] upperOpen, byte[] groupLowerClosed, byte[] groupUpperOpen)
            throws SQLException
    {
        SetOfKeys shardKeySet = shardingKeySetFactory.createKeySet(lowerClosed, upperOpen);
        SetOfKeys superKeySet = superShardingKeySetFactory.createKeySet(groupLowerClosed, groupUpperOpen);

        return isComposite ?
            new CompositeKeySet(superKeySet, shardKeySet)
                : (shardingMetadata != null ? shardKeySet : superKeySet);
    }

    public Shard getShard(String name) {
        return shards.get(name);
    }
    public Shard getShardForUpdate(String name) {
        return shards.computeIfAbsent(name, Shard::new);
    }

    public Collection<Chunk> getChunks() {
        return shards.values().stream().flatMap(x -> x.getChunks().stream())
                .collect(Collectors.toList());
    }

    private static SimpleKeySetFactory nullKeySetFactory = (x, y) -> null;
    private static SimpleKeySetFactory hashKeySetFactory = HashKeySet::create;

    private static SimpleKeySetFactory getKeySetFactory(ShardBy type) {
        switch (type) {
            case HASH: return hashKeySetFactory;
            default  : return nullKeySetFactory;
        }
    }

    public static OracleMetadataBuilder builder(int tableFamilyId, ShardBy superShardingType, ShardBy shardingType) {
        return new OracleMetadataBuilder(tableFamilyId).setShardingType(shardingType).setSuperShardingType(superShardingType);
    }

    public static class OracleMetadataBuilder {
        private final int tableFamilyId;
        private ShardBy shardingType = ShardBy.NONE;
        private ShardBy superShardingType = ShardBy.NONE;
        private final List<KeyColumn> superColumns = new ArrayList<>();
        private final List<KeyColumn> shardColumns = new ArrayList<>();

        public OracleMetadataBuilder(int tableFamilyId) {
            this.tableFamilyId = tableFamilyId;
        }

        public OracleMetadataBuilder setShardingType(ShardBy shardingType) {
            this.shardingType = shardingType;
            return this;
        }

        public OracleMetadataBuilder setSuperShardingType(ShardBy superShardingType) {
            this.superShardingType = superShardingType;
            return this;
        }

        public OracleMetadataBuilder addShardColumn(KeyColumn keyColumn) {
            shardColumns.add(keyColumn);
            return this;
        }

        public OracleMetadataBuilder addSuperColumn(KeyColumn keyColumn) {
            superColumns.add(keyColumn);
            return this;
        }

        public OracleShardingMetadata build() {
            return new OracleShardingMetadata(this);
        }
    }
}
