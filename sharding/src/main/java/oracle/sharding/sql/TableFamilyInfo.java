package oracle.sharding.sql;

import oracle.sharding.ShardBy;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by somestuff on 6/28/17.
 */
public class TableFamilyInfo implements Serializable {
    final int id;
    final String name;
    final String schemaName;
    final ShardBy superShardingType;
    final int superShardingColumns;
    final ShardBy shardingType;
    final int shardingColumns;
    final int metadataVersion;

    public TableFamilyInfo(ResultSet tfrs) throws SQLException {
        this.id = tfrs.getInt("tabfam_id");
        this.name = tfrs.getString("table_name");
        this.schemaName = tfrs.getString("schema_name");
        this.superShardingType = ShardBy.valueOf(tfrs.getString("group_type"));
        this.superShardingColumns = tfrs.getInt("group_col_num");
        this.shardingType = ShardBy.valueOf(tfrs.getString("shard_type"));
        this.shardingColumns = tfrs.getInt("shard_col_num");
        this.metadataVersion = tfrs.getInt("def_version");
    }
}
