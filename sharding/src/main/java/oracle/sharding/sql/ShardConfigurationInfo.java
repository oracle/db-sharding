/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.details.*;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/**
 * Created by itaranov on 6/28/17.
 */
public class ShardConfigurationInfo implements Serializable {
    private final InstanceInfo instanceInfo;
    private final TableFamilyInfo tableFamily;
    private final List<ColumnInfo> columnList = new ArrayList<>();
    private final List<OracleChunkInfo> chunkList = new ArrayList<>();
    private transient Function<OracleChunkInfo, Object> chunkAnnotateCallback = null;
    private final List<InstanceInfo> shards = new ArrayList<>();

    private ShardConfigurationInfo(TableFamilyInfo tableFamily, InstanceInfo instanceInfo) {
        this.tableFamily = tableFamily;
        this.instanceInfo = instanceInfo;
    }

    public static ShardConfigurationInfo loadFromDatabase(Connection connection, boolean readShards) throws SQLException {
        MetadataReader metadataReader = new MetadataReader(connection);
        InstanceInfo instanceInfo = new InstanceInfo(connection);
        ShardConfigurationInfo result = new ShardConfigurationInfo(metadataReader.getShardingInfo(), instanceInfo);
        ChunkReader chunkReader = new ChunkReader(connection);
        chunkReader.setInstanceInfo(instanceInfo);
        OracleShardingMetadata metadata = metadataReader.readMetadata();
        metadataReader.readShardColumns(result.tableFamily.id, result.columnList);
        chunkReader.readChunks(metadata, result.chunkList);

        if (readShards) {
            metadataReader.readShardData(result.shards);
        }

        return result;
    }

    public OracleShardingMetadata createMetadata() throws UnexpectedChunkConfigurationException
    {
        OracleShardingMetadata.OracleMetadataBuilder builder
                = OracleShardingMetadata.builder(tableFamily.id,
                    tableFamily.superShardingType, tableFamily.shardingType);

        for (ColumnInfo columnInfo : columnList) {
            OracleKeyColumn column = columnInfo.toOracleColumn();

            switch (columnInfo.level) {
                case 0:
                    builder.addSuperColumn(column);
                    break;
                case 1:
                    builder.addShardColumn(column);
                    break;
                default:
                    throw new UnexpectedChunkConfigurationException("Unexpected sharding level");
            }
        }

        OracleShardingMetadata result = builder.build();

        for (InstanceInfo instanceInfo : getInstances()) {
            result.getShardForUpdate(instanceInfo.getShardName()).setConnectionString(instanceInfo.connectionString);
        }

        return result;
    }

    public void updateRoutingTable(OracleRoutingTable routingTable)
            throws SQLException
    {
        Shard shard = instanceInfo.isCatalog ? null
                : routingTable.getMetadata().getShardForUpdate(instanceInfo.getShardName());

        final Set<Chunk> originalChunks = new HashSet<>(shard != null ? shard.getChunks() : routingTable.values());
        final Set<Chunk> updatedChunks = new HashSet<>();

        for (OracleChunkInfo chunkInfo : chunkList) {
            if (chunkInfo.update(routingTable.getMetadata())) {
                updatedChunks.add(chunkInfo.chunk);

                if (chunkAnnotateCallback != null) {
                    chunkInfo.chunk.annotation = chunkAnnotateCallback.apply(chunkInfo);
                }
            } else {
                originalChunks.remove(chunkInfo.chunk);
            }
        }

        routingTable.atomicUpdate(originalChunks, updatedChunks);
    }

    public OracleRoutingTable createRoutingTable() throws SQLException
    {
        OracleRoutingTable result = createMetadata().createRoutingTable();
        updateRoutingTable(result);
        return result;
    }

    public List<InstanceInfo> getInstances() {
        return Collections.unmodifiableList(shards);
    }

    public void setChunkAnnotateCallback(Function<OracleChunkInfo, Object> chunkAnnotateCallback) {
        this.chunkAnnotateCallback = chunkAnnotateCallback;
    }
}
