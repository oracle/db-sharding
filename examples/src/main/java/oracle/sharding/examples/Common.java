package oracle.sharding.examples;

/**
 * Created by somestuff on 8/1/17.
 */
public class Common {
    static final String connectionString = "jdbc:oracle:thin:@" +
            "(DESCRIPTION=(ADDRESS=(HOST=slc07efe)(PORT=1522)(PROTOCOL=tcp))" +
            "(CONNECT_DATA=(SERVICE_NAME=GDS$CATALOG.ORADBCLOUD)))";

    static final String username = "dyn";
    static final String password = "123";

    static final String routingTableFile = "routing.dat";

}
