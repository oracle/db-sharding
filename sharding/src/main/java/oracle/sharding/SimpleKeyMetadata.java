package oracle.sharding;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by somestuff on 4/1/17.
 */
public class SimpleKeyMetadata extends RoutingMetadata {
    final private ShardBy type;
    final private KeyColumn[] columns;

    public SimpleKeyMetadata(ShardBy type, List<KeyColumn> columns)
    {
        this.type = type;
        this.columns = columns.toArray(new KeyColumn[columns.size()]);
    }

    public SimpleKeyMetadata(ShardBy type, KeyColumn ... columns)
    {
        this.type = type;
        this.columns = columns;
    }

    public SimpleRoutingKey createKey(Object ... a) throws SQLException {
        if (a.length != columns.length) {
            throw new SQLException("Unexpected number of key values");
        }

        return createKey(a, 0);
    }

    public SimpleRoutingKey createKey(Object [] a, int begin) throws SQLException {
        if (type == ShardBy.NONE) {
            throw new SQLException("Sharding level not initialized");
        }

        SimpleRoutingKey key = new SimpleRoutingKey(this);
        int i = begin;

        for (KeyColumn c : columns) {
            key.values[i] = c.createValue(a[i]);
            ++i;
        }

        return key;
    }

    public Collection<KeyColumn> getColumns() {
        return Arrays.asList(columns);
    }

    public int getColumnCount() {
        return columns.length;
    }

    public ShardBy getType() {
        return type;
    }
}
