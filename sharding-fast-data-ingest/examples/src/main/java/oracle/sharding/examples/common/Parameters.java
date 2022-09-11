/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.common;

import oracle.util.metrics.Statistics;
import oracle.util.settings.Settings;
import oracle.util.settings.Settings.Property;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * A holder class for static fields with parameters.
 * The parameters can be specified as:
 * * Java system variables
 * * command line parameters starting with --
 * * loader.properties file
 *
 */
public class Parameters {
    public static String catalogConnectString = "jdbc:oracle:thin:@//localhost:1522/gds$catalog.oradbcloud";
    public static String outputDirectory = "/tmp/";
    public static String username = "test";
    public static String password = "123";
    public static String schemaName = "TEST";
    public static String tableName = "LOG";
    public static String routingTableFile = null;
    public static long entriesToGenerate = 100000;
    public static int parallelThreads = 16;

    @Property("output-dir")
    public void setOutputDirectory(String dir) {
        Parameters.outputDirectory = dir;
    }

    @Property("parallel")
    public void setParallelThreads(String parallel) {
        Parameters.parallelThreads = Integer.parseInt(parallel);
    }

    @Property("lines")
    public void setLines(String lineNumber) {
        Parameters.entriesToGenerate = Long.parseLong(lineNumber);
    }

    @Property("catalog")
    public void setConnectionString(String connectionString) {
        Parameters.catalogConnectString = connectionString;
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
    
    @Property("table")
    public void setTableName(String tableName) {
        Parameters.tableName = tableName;
    }
    

    @Property("route.cache")
    public void setRoutingTableFile(String routingTableFile) {
        Parameters.routingTableFile = routingTableFile.isEmpty() ? null : routingTableFile;
    }

    public static Connection getCatalogConnection() throws SQLException {
        return DriverManager.getConnection(Parameters.catalogConnectString, Parameters.username, Parameters.password);
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
        Properties properties = new Properties(System.getProperties());
        File propertyFile = new File(System.getProperty("loader.config", "loader.properties"));

        if (propertyFile.isFile()) {
            try (Reader reader = new BufferedReader(new FileReader(propertyFile))) {
                properties.load(reader);
            }
        }

        new Settings(properties).setProperties(new Parameters());
    }
}
