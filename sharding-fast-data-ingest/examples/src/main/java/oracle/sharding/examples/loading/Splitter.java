/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.loading;

import oracle.sharding.RoutingKey;
import oracle.sharding.details.ChunkTable;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.TaskBasedPartition;
import oracle.sharding.sql.MetadataReader;
import oracle.sharding.sql.ShardConfiguration;
import oracle.sharding.tools.SeparatedString;
import oracle.util.function.ConsumerWithError;
import oracle.util.function.FunctionWithError;
import oracle.util.settings.JSWrapper;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * TODO:
 * Input object splitter, based on javascript file description
 */
class Splitter {
    private PartitionEngine<SeparatedString> engine;
    private ChunkTable routingTable;
    private final List<BufferedWriter> outputWriters = new ArrayList<>();
    private final AtomicInteger fileCounter = new AtomicInteger(0);
    private long waitTimeout = 120;

    private final Collection<Object> inputFiles = new ArrayList<>();

    private int poolSize = 16;
    private int outputPoolSize = 16;

    public FunctionWithError<SeparatedString, RoutingKey, Exception> separatedStringKey(int n)
    {
        return FunctionWithError.create(
                (SeparatedString separatedString)
                        -> routingTable.getMetadata().createKey((separatedString).part(n)));
    }

    private final Map<String, String> shardConnectionStrings = new HashMap<>();

    private class ColumnDef {
        private final String name;
        private final int size;

        private ColumnDef(String name, int size) {
            this.name = name;
            this.size = size;
        }
    }

    private final List<ColumnDef> columnDefs = new ArrayList<>();

    public void loadMetadataFromCatalog(JSWrapper configurationMap)
    {
        Optional<JSWrapper> catalogOption = configurationMap.get("catalog");

        if (!catalogOption.isPresent()) {
            return;
        }

        JSWrapper catalogInfo = catalogOption.get();

        try {
            String connectionString = catalogInfo.get("catalogConnectString")
                    .orElseThrow(() -> new IllegalArgumentException("Catalog connection string must be provided"))
                    .asString();

            java.util.Properties info = new java.util.Properties();

            for (Map.Entry<String, Object> kv : catalogInfo.asMap().entrySet()) {
                if (!kv.getKey().equals("catalogConnectString")) {
                    info.setProperty(kv.getKey(), new JSWrapper(kv.getValue()).asString());
                }
            }

            String tableSchema = catalogInfo.get("schema")
                    .orElseThrow(() -> new IllegalArgumentException("Schema name ('schema') must be specified")).asString();
            String tableName = catalogInfo.get("table")
                    .orElseThrow(() -> new IllegalArgumentException("Table name ('table') must be specified")).asString();

            try (Connection catalogConnection = DriverManager.getConnection(connectionString, info)) {
                ShardConfiguration shardConfig = new MetadataReader(catalogConnection).getShardConfiguration();

                routingTable = shardConfig.createMetadata().createChunkTable();

                shardConfig.getShards().forEach(
                        instanceInfo -> shardConnectionStrings.put(instanceInfo.getShardName(),
                                instanceInfo.getConnectionString()));

                try (PreparedStatement statement = catalogConnection
                        .prepareStatement("select column_name, data_type, data_length from dba_tab_columns" +
                            " where owner=? and table_name=?");
                     )
                {
                    statement.setString(1, tableSchema);
                    statement.setString(2, tableName);

                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            int size = rs.getInt(3);

                            switch (rs.getInt(2)) {
                                default:
                            }

                            columnDefs.add(new ColumnDef(rs.getString(1), size));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<String> dynamicInputObjectResolve(Object x) {
        if (x instanceof String) {
            x = new File((String) x);
        }

        if (x instanceof File || x instanceof URL) {
            try {
                BufferedReader reader = new BufferedReader(
                    x instanceof File ? new FileReader((File) x)
                        : new InputStreamReader(((URL) x).openStream()));

                return reader.lines().onClose(() -> {
                    try { reader.close(); } catch (IOException ignore) { }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (x instanceof Stream) {
            return ((Stream<Object>) x).map(Object::toString);
        } else {
            throw new RuntimeException("Unknown input object");
        }
    }

    public String getShardConnectionString(String name) {
        return shardConnectionStrings.get(name);
    }

    public void run(JSWrapper configurationScript) throws Exception {
        JSWrapper configurationMap = configurationScript.get("configuration")
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found"))
            .callOrGetValue(this, this);

        loadMetadataFromCatalog(configurationScript);

        configurationMap.get("sink").ifPresent(x ->
                engine.setCreateSinkFunction(x.asFunction(configurationMap)));

        configurationMap.get("onCreateSink").ifPresent(x ->
                engine.setCreateSinkFunction(x.asFunction(configurationMap)));

        configurationMap.get("keyIndex").ifPresent(x ->
                engine.getSplitter().setGetKey(separatedStringKey(x.asNumber(0).intValue()).onErrorRuntimeException()));

        configurationMap.get("onGetKey").ifPresent(x ->
                engine.getSplitter().setGetKey(x.asFunction(configurationMap)));

        int maxColumns = configurationMap.get("maxColumns")
                .orElse(JSWrapper.nullObject()).asNumber(-1).intValue();

        inputFiles.addAll(configurationMap.get("source")
            .orElseThrow(() -> new java.lang.IllegalArgumentException("Configuration not found"))
            .asCollection());

        try {
            engine = new TaskBasedPartition<>(routingTable, Executors.newFixedThreadPool(outputPoolSize));

            inputFiles.stream().flatMap(this::dynamicInputObjectResolve)
                    .map(s -> new SeparatedString(s, '|', maxColumns))
                    .forEach(s -> engine.getSplitter().feed(s));

            engine.waitAndClose(waitTimeout * 1000);
        } finally {
            engine.close();
            outputWriters.forEach((ConsumerWithError.create(BufferedWriter::close).onErrorPrint()));
        }
    }

    public static void main(String [] args)
    {
        try {
            new Splitter().run(JSWrapper.eval(new FileReader("/home/itaranov/test.js")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
