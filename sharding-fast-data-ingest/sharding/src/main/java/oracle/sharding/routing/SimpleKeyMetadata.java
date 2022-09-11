/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/
package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.RoutingMetadata;
import oracle.sharding.ShardBy;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * General multi-column key factory.
 */
public class SimpleKeyMetadata implements RoutingMetadata {
    final protected ShardBy type;
    final protected KeyColumn[] columns;

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

    public RoutingKey createKey(Object ... a) throws SQLException {
        if (a.length != columns.length) {
            throw new SQLException("Unexpected number of key values");
        }

        return createKey(a, 0);
    }

    public RoutingKey createKey(Object [] a, int begin) throws SQLException {
        if (type == ShardBy.NONE) {
            throw new SQLException("Sharding level not initialized");
        }

        MultiColumnKey key = new MultiColumnKey(this);
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
