/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.OracleShardingMetadata;
import oracle.sharding.ShardConfigurationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * SQL queries to read metadata from the catalog or shards.
 */
public class MetadataReader {
    private final Connection connection;
    private boolean isCatalog;
    private ShardConfiguration tfConfig = null;

    /**
     * Create a metadata reader given a specific connection
     * @param connection to a database
     */
    public MetadataReader(Connection connection) {
        this.connection = connection;
    }

    /**
     * Set table family scope to the specific table family id
     * @param tableFamily table family ID
     */
    public void setTableFamily(int tableFamily) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
                "select tabfam_id, table_name, schema_name, group_type, group_col_num, "
                + " shard_type, shard_col_num, def_version from local_chunk_types "
                + " where tabfam_id=:1"))
        {
            statement.setInt(1, tableFamily);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    tfConfig = new ShardConfiguration(rs);
                } else {
                    throw new ShardConfigurationException("Table family not found");
                }
            }
        }
    }

    /**
     * Set table family scope to the specific table family name
     *
     * @param schemaName schema name
     * @param tableName table family name (not always the same as root table name)
     */
    public void setTableFamily(String schemaName, String tableName) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
                "select tabfam_id, table_name, schema_name, group_type, group_col_num, "
                + " shard_type, shard_col_num, def_version from local_chunk_types "
                + " where table_name=:1 and schema_name=:2"))
        {
            statement.setString(1, tableName);
            statement.setString(2, schemaName);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    tfConfig = new ShardConfiguration(rs);
                } else {
                    throw new ShardConfigurationException("Table family not found");
                }

                if (rs.next()) {
                    throw new ShardConfigurationException("Ambiguous table family specified");
                }
            }
        }
    }

    public void setTableFamily() throws SQLException {
        Collection<ShardConfiguration> tfs = readTableFamilies();

        if (tfs.size() > 1) {
            throw new ShardConfigurationException("Multiple table families exist");
        }

        if (tfs.size() == 0) {
            throw new ShardConfigurationException("No table family information");
        }

        tfConfig = tfs.iterator().next();
    }

    private void checkTableFamily() throws SQLException {
        if (tfConfig == null) {
            setTableFamily();
        }
    }

    /**
     * Read the list of databases known to the catalog (if connected to the catalog)
     * If connected to shards, empty list is most likely returned.
     *
     * @return the collection of database
     * @throws SQLException in case of SQL error
     */
    public Collection<InstanceInfo> readShardInformation() throws SQLException
    {
        Collection<InstanceInfo> instanceList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "select db_unique_name, connect_string, is_primary from sha_databases"))
        {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    InstanceInfo instance = new InstanceInfo(rs.getString(1), rs.getString(2));
                    instance.setPrimary(rs.getString(3).equalsIgnoreCase("Y"));

                    instanceList.add(instance);
                }
            }
        }

        return instanceList;
    }

    /**
     * Read the list of shard columns
     *
     * @return the collection of shard column descriptions
     * @throws SQLException in case of SQL error
     */
    public Collection<ColumnInfo> readShardColumns() throws SQLException
    {
        checkTableFamily();

        Collection<ColumnInfo> columnList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "select tabfam_id, shard_level, col_idx_in_key, col_name, eff_type, character_set, "
                        + " col_size from local_chunk_columns where tabfam_id = :1 "
                        + " order by shard_level, col_idx_in_key"))
        {
            statement.setInt(1, tfConfig.tableFamilyId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    columnList.add(new ColumnInfo(
                            rs.getInt("eff_type"),
                            rs.getInt("character_set"),
                            rs.getInt("col_size"),
                            rs.getInt("shard_level"),
                            rs.getInt("col_idx_in_key")));
                }
            }
        }

        return columnList;
    }

    /**
     * Read LOCAL_CHUNKS view and return the chunk collection
     * @return the chunk collection
     * @throws SQLException in case of SQL error
     */
    public Collection<ChunkInfo> readChunks() throws SQLException {
        checkTableFamily();

        Collection<ChunkInfo> chunkList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "select chunk_name, grp_id, chunk_id, chunk_unique_id, " +
                        " shard_key_low, shard_key_high, group_key_low, group_key_high, " +
                        " priority, state, shard_name, shardspace_name, " +
                        " (select dd.flags from gsmadmin_internal.database dd where dd.name=c.shard_name) as database_state " +
                        " from local_chunks c where tabfam_id=:1 " +
                        " order by grp_id, chunk_id"))
        {
            statement.setInt(1, tfConfig.tableFamilyId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String shardName = rs.getString("shard_name");

                    if (shardName == null && isCatalog) {
                        throw new ShardConfigurationException("SHARD_NAME information was not found. " +
                                "User is not granted with gsmadmin_role privileges, or you are " +
                                " connected to a shard, when catalog connection is expected.");
                    }

                    if ((shardName == null || shardName.length() == 0) && !isCatalog)
                    {
                        throw new ShardConfigurationException("Reading local shard information is not supported");
                    }

                    chunkList.add(new ChunkInfo(rs, shardName));
                }
            }
        }

        return chunkList;
    }

    /**
     * Read table families and return the list of table family descriptions
     *
     * @return table family descriptor collection
     * @throws SQLException in case of SQL error
     */
    public  Collection<ShardConfiguration> readTableFamilies() throws SQLException {
        Collection<ShardConfiguration> tfList = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
            "select tabfam_id, table_name, schema_name, group_type, group_col_num, "
             + " shard_type, shard_col_num, def_version from local_chunk_types "
             + " order by tabfam_id"))
        {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    tfList.add(new ShardConfiguration(rs));
                }
            }
        }

        return tfList;
    }

    public ShardConfiguration getShardConfiguration() throws SQLException
    {
        checkTableFamily();
        fillConfig();
        return tfConfig;
    }

    private void fillConfig() throws SQLException
    {
        checkTableFamily();

        tfConfig.columns.clear();
        tfConfig.columns.addAll(readShardColumns());

        tfConfig.databases.clear();
        tfConfig.databases.addAll(readShardInformation());

        tfConfig.chunks.clear();
        tfConfig.chunks.addAll(readChunks());
    }

    public OracleShardingMetadata getMetadata() throws SQLException
    {
        fillConfig();
        return tfConfig.createMetadata();
    }

    public void updateMetadata(OracleShardingMetadata metadata) throws SQLException
    {
        fillConfig();
        tfConfig.updateMetadata(metadata);
    }
}
