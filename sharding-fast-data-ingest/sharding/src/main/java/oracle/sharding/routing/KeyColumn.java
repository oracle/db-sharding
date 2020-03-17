/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;

import java.sql.SQLException;

/**
 * Interface for constructing a single value routing key
 */
public interface KeyColumn {
    RoutingKey createValue(Object from) throws SQLException;
}
