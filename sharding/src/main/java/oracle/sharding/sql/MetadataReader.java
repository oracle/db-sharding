package oracle.sharding.sql;

import oracle.sharding.details.OracleKeyColumn;
import oracle.sharding.details.OracleShardingMetadata;
import oracle.util.sql.QueryStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by somestuff on 4/3/17.
 */
public class MetadataReader {
    private final Connection connection;

    public MetadataReader(Connection connection) {
        this.connection = connection;
    }

    final Map<Integer, TableFamilyInfo> tfmap = new HashMap<>();

    public Collection<InstanceInfo> readShardData(Collection<InstanceInfo> instanceList) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
                "select db_unique_name, connect_string, is_primary from sha_databases"))
        {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    InstanceInfo instance = new InstanceInfo(rs.getString(1), null);
                    instance.connectionString = rs.getString(2);
                    instanceList.add(instance);
                }
            }
        }

        return instanceList;
    }

    public Stream<InstanceInfo> readShardData() throws SQLException
    {
        return QueryStream.create(connection.prepareStatement(
                "select db_unique_name, connect_string, is_primary from sha_databases")).map(
                (ResultSet x) -> {
                    try {
                        return new InstanceInfo(x.getString(1), null, x.getString(2));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void readTableFamilies() throws SQLException {
        tfmap.clear();

        try (PreparedStatement statement = connection.prepareStatement(
            "select tabfam_id, table_name, schema_name, group_type, group_col_num, "
             + " shard_type, shard_col_num, def_version from local_chunk_types "
             + " order by tabfam_id"))
        {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TableFamilyInfo tf = new TableFamilyInfo(rs);
                    tfmap.put(tf.id, tf);
                }
            }
        }
    }

    private int getLastTf() throws SQLException {
        Optional<Integer> tfIdMax = tfmap.keySet().stream().max(Integer::compareTo);

        if (!tfIdMax.isPresent()) {
            throw new SQLException("TODO");
        }

        return tfIdMax.get();
    }

    public TableFamilyInfo getShardingInfo() throws SQLException
    {
        return getShardingInfo(-1);
    }

    public TableFamilyInfo getShardingInfo(int tableFamilyId) throws SQLException
    {
        if (tfmap.isEmpty()) {
            readTableFamilies();
        }

        if (tableFamilyId == -1) {
            tableFamilyId = getLastTf();
        }

        return tfmap.get(tableFamilyId);
    }

    public OracleShardingMetadata readMetadata() throws SQLException
    {
        return readMetadata(-1);
    }

    public OracleShardingMetadata readMetadata(int tableFamilyId) throws SQLException
    {
        TableFamilyInfo tf = getShardingInfo(tableFamilyId);

        if (tableFamilyId == -1) { tableFamilyId = tf.id; }

        OracleShardingMetadata.OracleMetadataBuilder builder
                = OracleShardingMetadata.builder(tableFamilyId, tf.superShardingType, tf.shardingType);

        try (PreparedStatement statement = connection.prepareStatement(
                "select tabfam_id, shard_level, col_idx_in_key, col_name, eff_type, character_set, "
                    + " col_size from local_chunk_columns where tabfam_id = :1 "
                    + " order by shard_level, col_idx_in_key"))
        {
            statement.setInt(1, tf.id);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    OracleKeyColumn column =
                            OracleKeyColumn.createOracleKeyColumn(
                                    rs.getInt("eff_type"), rs.getInt("character_set"), rs.getInt("col_size"));

                    switch (rs.getInt("shard_level")) {
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
            }
        }

        return builder.build();
    }

    public Collection<ColumnInfo> readShardColumns(int tableFamilyId, Collection<ColumnInfo> columnList) throws SQLException
    {
        TableFamilyInfo tf = getShardingInfo(tableFamilyId);
        if (tableFamilyId == -1) { tableFamilyId = tf.id; }

        try (PreparedStatement statement = connection.prepareStatement(
                "select tabfam_id, shard_level, col_idx_in_key, col_name, eff_type, character_set, "
                        + " col_size from local_chunk_columns where tabfam_id = :1 "
                        + " order by shard_level, col_idx_in_key"))
        {
            statement.setInt(1, tf.id);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    columnList.add(new ColumnInfo(
                            rs.getInt("eff_type"), rs.getInt("character_set"), rs.getInt("col_size"),
                            rs.getInt("shard_level"), rs.getInt("col_idx_in_key")));
                }
            }
        }

        return columnList;
    }
}
