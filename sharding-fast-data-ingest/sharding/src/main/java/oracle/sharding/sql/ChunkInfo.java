/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.SetOfKeys;
import oracle.sharding.details.Chunk;
import oracle.sharding.details.Shard;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Chunk information, modelling LOCAL_CHUNKS view
 */
public class ChunkInfo implements Serializable {
    public String shardName;
    public byte[] shardKeyLo;
    public byte[] shardKeyHi;
    public byte[] groupKeyLo;
    public byte[] groupKeyHi;

    public int chunkId;
    public int groupId;
    public int uniqueId;

    public String name;
    public int priority;
    public int state;

    public ChunkInfo() {

    }

    public boolean isPrimary() {
        return priority == 0 && state == 0;
    }

    public ChunkInfo(ResultSet rs, String shardName) throws SQLException {
        this.shardName = shardName != null ? shardName : rs.getString("shard_name");

        this.groupKeyLo = rs.getBytes("group_key_low");
        this.groupKeyHi = rs.getBytes("group_key_high");

        this.shardKeyLo = rs.getBytes("shard_key_low");
        this.shardKeyHi = rs.getBytes("shard_key_high");

        this.chunkId = rs.getInt("chunk_id");
        this.groupId = rs.getInt("grp_id");

        this.uniqueId = rs.getInt("chunk_unique_id");
        this.name = rs.getString("chunk_name");
        this.priority = rs.getInt("priority");
        this.state = rs.getInt("state");
    }


    public SetOfKeys createKeySet(OracleShardingMetadata metadata) throws SQLException
    {
        return metadata.createKeySet(shardKeyLo, shardKeyHi, groupKeyLo, groupKeyHi);
    }

    public Chunk createChunk(Shard shard, OracleShardingMetadata metadata) throws SQLException
    {
    	return new Chunk(chunkId, uniqueId, shard, groupId, createKeySet(metadata));
    }
}
