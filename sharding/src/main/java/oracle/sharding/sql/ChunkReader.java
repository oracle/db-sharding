/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.details.OracleRoutingTable;
import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleShardingMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by itaranov on 4/3/17.
 */
public class ChunkReader {
    private final Connection connection;
    private InstanceInfo instanceInfo;
    private int tableFamilyId = -1;

    public ChunkReader(Connection connection) {
        this.connection = connection;
    }

    public int getTableFamilyId() {
        return tableFamilyId;
    }

    public void setTableFamilyId(int tableFamilyId) {
        this.tableFamilyId = tableFamilyId;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public void setInstanceInfo(InstanceInfo instanceInfo) {
        this.instanceInfo = instanceInfo;
    }

    void readChunks(OracleShardingMetadata metadata, Collection<OracleChunkInfo> chunks) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select chunk_name, grp_id, chunk_id, chunk_unique_id, " +
                    " shard_key_low, shard_key_high, group_key_low, group_key_high, " +
                    " priority, state, shard_name, shardspace_name, " +
                    " (select dd.flags from gsmadmin_internal.database dd where dd.name=c.shard_name) as database_state " +
                    " from local_chunks c " + (tableFamilyId == -1 ? " " : " where tabfam_id=:1 ") +
                    " order by grp_id, chunk_id"))
        {
            if (tableFamilyId != -1) {
                statement.setInt(1, getTableFamilyId());
            }

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String shardName = rs.getString("shard_name");

                    if (shardName == null && instanceInfo == null) {
                        throw new SQLException("SHARD_NAME information was not found. " +
                                "User is not granted with gsmadmin_role privileges, or you are " +
                                " connected to a shard, when catalog connection is expected.");
                    }

                    if ((shardName == null || shardName.length() == 0)
                            && (instanceInfo != null)
                            && !instanceInfo.isCatalog)
                    {
                        shardName = instanceInfo.getShardName();
                    }

                    chunks.add(new OracleChunkInfo(metadata, rs, shardName));
                }
            }
        }
    }

    public void updateRoutingTable(OracleRoutingTable routingTable, Function<OracleChunkInfo, Object> annotateCallback) throws SQLException
    {
        final ArrayList<OracleChunkInfo> chunks = new ArrayList<>();
        readChunks(routingTable.getMetadata(), chunks);

        final Set<Chunk> newChunks = new HashSet<>();
        final Set<Chunk> originalChunks = chunks.stream().map(OracleChunkInfo::getShard)
                .distinct().flatMap(shard -> shard.getChunks().stream())
                .collect(Collectors.toSet());

        for (OracleChunkInfo chunkInfo : chunks) {
            if (chunkInfo.update(routingTable.getMetadata())) {
                newChunks.add(chunkInfo.chunk);

                if (annotateCallback != null) {
                    chunkInfo.chunk.annotation = annotateCallback.apply(chunkInfo);
                }
            } else {
                originalChunks.remove(chunkInfo.chunk);
            }
        }

        routingTable.atomicUpdate(originalChunks, newChunks);
    }
}
