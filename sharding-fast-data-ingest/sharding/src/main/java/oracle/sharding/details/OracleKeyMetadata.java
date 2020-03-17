/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding.details;

import oracle.sharding.routing.KeyColumn;
import oracle.sharding.RoutingKey;
import oracle.sharding.ShardBy;
import oracle.sharding.routing.SimpleKeyMetadata;

import java.sql.SQLException;
import java.util.List;

/**
 * General sharding key factory
 */
public class OracleKeyMetadata extends SimpleKeyMetadata {
    public OracleKeyMetadata(ShardBy type, List<KeyColumn> columns) {
        super(type, columns);
    }

    public OracleKeyMetadata(ShardBy type, KeyColumn... columns) {
        super(type, columns);
    }

    @Override
    public RoutingKey createKey(Object[] a, int begin) throws SQLException {
        if (columns.length == 1) {
            return columns[0].createValue(a[begin]);
        } else {
            return super.createKey(a, begin);
        }
    }
}
