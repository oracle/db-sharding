package oracle.sharding.examples;

import oracle.util.metrics.Statistics;
import oracle.util.settings.Settings;
import oracle.util.settings.Settings.Property;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * Created by somestuff on 8/1/17.
 */
public class Parameters {
    static String connectionString = "jdbc:oracle:thin:@" +
            "(DESCRIPTION=(ADDRESS=(HOST=slc07efe)(PORT=1522)(PROTOCOL=tcp))" +
            "(CONNECT_DATA=(SERVICE_NAME=GDS$CATALOG.ORADBCLOUD)))";

    static String username = "dyn";
    static String password = "123";
    static String schemaName;
    static String routingTableFile = null;
    static long entriesToGenerate = 100000;
    static int parallelThreads = 16;

    @Property("parallel")
    public void setParallelThreads(String parallel) {
        Parameters.parallelThreads = Integer.parseInt(parallel);
    }

    @Property("lines")
    public void setLines(String lineNumber) {
        Parameters.entriesToGenerate = Long.parseLong(lineNumber);
    }

    @Property("connect")
    public void setConnectionString(String connectionString) {
        Parameters.connectionString = connectionString;
    }

    @Property("password")
    public void setPassword(String password) {
        Parameters.password = password;
    }

    @Property("username")
    public void setUsername(String username) {
        Parameters.username = username;
    }

    @Property("schema")
    public void setSchemaName(String schemaName) {
        Parameters.schemaName = schemaName;
    }

    @Property("route.cache")
    public void setRoutingTableFile(String routingTableFile) {
        Parameters.routingTableFile = routingTableFile.isEmpty() ? null : routingTableFile;
    }

    @Property("statistics.period")
    public void setStatisticsPeriod(String period) {
        long periodLong = Long.parseLong(period);

        if (periodLong != 0) {
            new Timer(true).scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.println(Statistics.getGlobal().getMetrics().stream()
                            .map(Statistics.Metric::toString).collect(Collectors.joining("; ")));
                    System.out.flush();
                }
            }, periodLong, periodLong);
        }
    }

    public static void init(String[] args)
            throws InvocationTargetException, IllegalAccessException, IOException {
        Properties properties;

        try (Reader reader = new BufferedReader(new FileReader(
                new File(System.getProperty("loader.config", "loader.properties")))))
        {
            properties = new Properties(System.getProperties());
            properties.load(reader);
        }

        new Settings(properties).setProperties(new Parameters());
    }
}
