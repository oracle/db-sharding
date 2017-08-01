package oracle.sharding.sql;

import oracle.sharding.SetOfKeys;
import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleShardingMetadata;
import oracle.sharding.details.Shard;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by somestuff on 6/28/17.
 */
public class OracleChunkInfo implements Serializable {
    final public String shardName;
    final public SetOfKeys keySet;

    final public int chunkId;
    final public int groupId;
    final public int uniqueId;

    final public String name;
    final public int priority;
    final public int state;

    transient Shard shard;
    transient Chunk chunk;

    OracleChunkInfo(OracleShardingMetadata metadata, ResultSet rs, String shardName) throws SQLException {
        this.shardName = shardName != null ? shardName : rs.getString("shard_name");

        keySet = metadata.createKeySet(
                rs.getBytes("shard_key_low"), rs.getBytes("shard_key_high"),
                rs.getBytes("group_key_low"), rs.getBytes("group_key_high"));

        this.chunkId = rs.getInt("chunk_id");

        this.groupId = rs.getInt("grp_id");

        this.uniqueId = rs.getInt("chunk_unique_id");
        this.name = rs.getString("chunk_name");
        this.priority = rs.getInt("priority");
        this.state = rs.getInt("state");

        this.shard = metadata.getShardForUpdate(shardName);
    }

    public boolean update(OracleShardingMetadata metadata) throws SQLException {
        this.shard = metadata.getShardForUpdate(shardName);

        if (this.chunk == null) {
            this.chunk = new Chunk(uniqueId, shard, groupId, keySet);
            this.chunk.setChunkId(chunkId);
            return true;
        }

        if (!this.chunk.getKeySet().equals(this.keySet)) {
            this.chunk = this.chunk.updateKey(this.keySet);
            this.chunk.setChunkId(chunkId);
            return true;
        }

        return false;
    }

    public Shard getShard() {
        return shard;
    }

    public Chunk getChunk() {
        return chunk;
    }
}
