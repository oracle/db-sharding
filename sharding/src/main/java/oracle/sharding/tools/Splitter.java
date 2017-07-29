package oracle.sharding.tools;

import oracle.sharding.RoutingKey;
import oracle.sharding.details.Chunk;
import oracle.sharding.details.OracleRoutingTable;
import oracle.sharding.details.Shard;
import oracle.sharding.splitter.PartitionEngine;
import oracle.sharding.splitter.TaskBasedPartition;
import oracle.sharding.sql.MetadataReader;
import oracle.sharding.sql.ShardConfigurationInfo;
import oracle.util.function.ConsumerWithError;
import oracle.util.function.FunctionWithError;
import oracle.util.settings.JSWrapper;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Input object splitter, based on javascript file description
 * 
 * Created by itaranov on 6/20/17.
 */
public class Splitter {
    private PartitionEngine<SeparatedString> engine;
    private OracleRoutingTable routingTable;
    private final List<BufferedWriter> outputWriters = new ArrayList<>();
    private final AtomicInteger fileCounter = new AtomicInteger(0);
    private long waitTimeout = 120;

    private final Collection<Object> inputFiles = new ArrayList<>();

    private int poolSize = 16;
    private int outputPoolSize = 16;

    public Consumer<List<SeparatedString>> createOutputFile(Shard shard) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("/tmp/test-" + shard.getName() + "-" + fileCounter.incrementAndGet()));

            outputWriters.add(writer);

            return x -> x.forEach(ConsumerWithError.create((SeparatedString y)
                        -> writer.append(y.toCharSequence()).append('\n'))
                    .onErrorRuntimeException());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public Consumer<List<SeparatedString>> createOutputFile(Chunk chunk) {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter("/tmp/test-CHUNK_" + chunk.getChunkUniqueId()
                            + "-" + fileCounter.incrementAndGet()));

            outputWriters.add(writer);

            return x -> x.forEach(ConsumerWithError.create((SeparatedString y)
                    -> writer.append(y.toCharSequence()).append('\n'))
                    .onErrorRuntimeException());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class SQLSink implements AutoCloseable, ConsumerWithError<List<SeparatedString>, SQLException> {
        private final Connection connection;
        private final PreparedStatement statement;

        public SQLSink(String connectionString, String statement) throws SQLException {
            this.connection = DriverManager.getConnection(connectionString, "u1", "123");
            this.statement = connection.prepareStatement(statement);
        }

        @Override
        public void close() throws Exception {
            try { statement.close(); } catch (Exception ignore) {}
            connection.close();
        }

        @Override
        public void accept(List<SeparatedString> separatedStrings) throws SQLException {
            connection.setAutoCommit(false);
            connection.commit();
            int n = statement.getParameterMetaData().getParameterCount();

            for (SeparatedString s : separatedStrings) {
//                String data = s.toString();
                for (int i = 0; i < n; ++i) {
                    statement.setString(i + 1, s.part(i));
                }

                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
            System.out.println("Loaded");
            System.out.flush();
        }
    }

    public FunctionWithError<SeparatedString, RoutingKey, Exception> separatedStringKey(int n)
    {
        return FunctionWithError.create(
                (SeparatedString separatedString)
                        -> routingTable.getMetadata().createKey((separatedString).part(n)));
    }

    private final Map<String, String> shardConnectionStrings = new HashMap<>();

    public void loadMetadataFromCatalog(JSWrapper catalogInfo)
    {
        try {
            String connectionString = catalogInfo.get("connectionString")
                    .orElseThrow(() -> new IllegalArgumentException("Catalog connection string must be provided"))
                    .asString();

            java.util.Properties info = new java.util.Properties();

            for (Map.Entry<String, Object> kv : catalogInfo.asMap().entrySet()) {
                if (!kv.getKey().equals("connectionString")) {
                    info.setProperty(kv.getKey(), new JSWrapper(kv.getValue()).asString());
                }
            }

            try (Connection catalogConnection = DriverManager.getConnection(connectionString, info)) {
                routingTable = ShardConfigurationInfo.loadFromDatabase(catalogConnection).createRoutingTable();

                new MetadataReader(catalogConnection).readShardData().forEach(
                        instanceInfo -> shardConnectionStrings.put(instanceInfo.getShardName(),
                                instanceInfo.getConnectionString()));
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
                    try {
                        reader.close();
                    } catch (IOException ignore) {
                        /* */
                    }
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

    /*
    private Function<Object, Consumer<List<SeparatedString>>> resolveSink(String sinkName, JSWrapper) {
        if (sinkName == )
        return x -> ;
    }
    */

    public String getShardConnectionString(String name) {
        return shardConnectionStrings.get(name);
    }

    public void run(JSWrapper configurationScript) throws Exception {
        JSWrapper configurationMap = configurationScript.get("configuration")
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found"))
            .callOrGetValue(this, this);

        configurationMap.get("catalog").ifPresent(this::loadMetadataFromCatalog);
        configurationMap.get("sink").ifPresent(x ->
                engine.setCreateSinkFunction(x.asFunction(configurationMap)));
        configurationMap.get("onCreateSink").ifPresent(x ->
                engine.setCreateSinkFunction(x.asFunction(configurationMap)));
        configurationMap.get("keyIndex").ifPresent(x ->
                engine.getSplitter().setGetKey(separatedStringKey(x.asNumber(0).intValue()).onErrorRuntimeException()));
        configurationMap.get("onGetKey").ifPresent(x ->
                engine.getSplitter().setGetKey(x.asFunction(configurationMap)));

        int maxColumns = configurationMap.get("maxColumns").orElse(JSWrapper.nullObject()).asNumber(-1).intValue();

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
