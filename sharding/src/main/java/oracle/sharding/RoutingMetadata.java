package oracle.sharding;

import java.sql.SQLException;

/**
 * Created by somestuff on 4/1/17.
 */
public abstract class RoutingMetadata {
    public abstract RoutingKey createKey(Object [] a, int begin) throws SQLException;
    public abstract RoutingKey createKey(Object ... a) throws SQLException;
}
