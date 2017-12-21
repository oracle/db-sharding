/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.ShardBy;
import oracle.sharding.ShardConfigurationException;
import oracle.sharding.details.OracleKeyColumn;
import oracle.sharding.details.Shard;
import oracle.sharding.routing.SimpleKeyMetadata;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Catalog configuration, which contains information on chunks,
 * shards and table family, etc.
 *
 * The class is serializable and it does not require OJDBC, so
 * the configuration can be shipped over between hosts even if
 * they are not connected to the database.
 *
 * NOTE (Usage notice) : it does not make much sense to update this structure,
 * if the application receive metadata update. It should update
 * OracleShardingMetadata object or Routing table directly.
 */
public class ShardConfiguration implements Serializable {
    public int tableFamilyId;
    public String name;
    public String schemaName;
    public ShardBy superShardingType;
    public ShardBy shardingType;

    public int metadataVersion;

    final List<ColumnInfo> columns = new ArrayList<>();
    final List<ChunkInfo> chunks = new ArrayList<>();
    final List<InstanceInfo> databases = new ArrayList<>();

    /**
     * Get a modifiable list of shards (if available)
     * @return the list of shards
     */
    public Collection<InstanceInfo> getShards() {
        return databases;
    }

    /**
     * Get a modifiable list of sharding columns
     * @return the list of shard columns
     */
    public Collection<ColumnInfo> getColumns() {
        return columns;
    }

    /**
     * Get a modifiable list of chunks
     * @return the list of chunks
     */
    public Collection<ChunkInfo> getChunks() {
        return chunks;
    }

    /**
     * Create empty shard configuration object
     */
    public ShardConfiguration() {
    }


    public ShardConfiguration(int tableFamilyId, String name, String schemaName,
        ShardBy superShardingType, ShardBy shardingType, int metadataVersion)
    {
        this.tableFamilyId = tableFamilyId;
        this.name = name;
        this.schemaName = schemaName;
        this.superShardingType = superShardingType;
        this.shardingType = shardingType;
        this.metadataVersion = metadataVersion;
    }

    ShardConfiguration(ResultSet tfrs) throws SQLException {
        this(tfrs.getInt("tabfam_id"),tfrs.getString("table_name"), tfrs.getString("schema_name"),
                ShardBy.valueOf(tfrs.getString("group_type")), ShardBy.valueOf(tfrs.getString("shard_type")),
                    tfrs.getInt("def_version"));
    }

    /**
     * Create metadata based on the configuration.
     *
     * @return Oracle sharding metadata instance
     * @throws ShardConfigurationException if the configuration is
     *      incompatible with the current implementation
     */
    public OracleShardingMetadata createMetadata() throws SQLException
    {
        OracleShardingMetadata result = createKeyOnlyMetadata();

        updateMetadata(result);

        return result;
    }

    /**
     * Create metadata based on the configuration without chunk and shard information
     *
     * @return Oracle sharding metadata instance
     * @throws ShardConfigurationException if the configuration is
     *      incompatible with the current implementation
     */
    public OracleShardingMetadata createKeyOnlyMetadata() throws SQLException
    {
        OracleShardingMetadata.Builder builder
                = OracleShardingMetadata.builder(superShardingType, shardingType);

        for (ColumnInfo columnInfo : columns) {
            OracleKeyColumn column = columnInfo.toOracleColumn();

            switch (columnInfo.level) {
                case 0:
                    builder.addSuperColumn(column);
                    break;
                case 1:
                    builder.addShardColumn(column);
                    break;
                default:
                    throw new ShardConfigurationException("Unexpected sharding level");
            }
        }

        return builder.build();
    }

    public void updateMetadata(OracleShardingMetadata metadata)
            throws SQLException
    {
        /* FIXME : remove deleted shards */

        for (InstanceInfo shardInfo : databases) {
            Shard shard = metadata.getShardForUpdate(shardInfo.getShardName());
            shard.setPrimary(shardInfo.isPrimary());
            shard.setConnectionString(shardInfo.getConnectionString());
            shard.clearChunks();
        }

        for (ChunkInfo chunk : chunks) {
            Shard shard = metadata.getShardForUpdate(chunk.shardName);
            shard.addChunk(chunk.createChunk(shard, metadata));
        }

        metadata.updateChunkTables();
    }

    public void verifyKeyMetadata(OracleShardingMetadata metadata) throws ShardConfigurationException
    {
        SimpleKeyMetadata shardKeyInfo = metadata.getShardingMetadata();
        SimpleKeyMetadata superKeyInfo = metadata.getSuperShardingMetadata();

        if (superKeyInfo.getType() != superShardingType) {
            throw new ShardConfigurationException("Super sharding type does not match");
        }

        if (shardKeyInfo.getType() != shardingType) {
            throw new ShardConfigurationException("Sharding type does not match");
        }


    }
}
