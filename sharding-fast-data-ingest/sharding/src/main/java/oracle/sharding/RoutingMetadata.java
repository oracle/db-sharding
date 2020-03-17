/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import java.sql.SQLException;

/**
 * Routing metadata, which would normally contain type information
 * and sharding method information
 */
public interface RoutingMetadata {
    /**
     * Create a routing key given an array of corresponding values, making
     * required transformations if necessary.
     *
     * @param a array of values
     * @param begin number of elements to skip from the beginning
     * @return a valid routing key, corresponding to the metadata
     * @throws SQLException if values does not satisfy metadata requirements
     */
    RoutingKey createKey(Object [] a, int begin) throws SQLException;

    /**
     * Create a routing key given an array of corresponding values, making
     * required transformations if necessary.
     *
     * @param a values
     * @return a valid routing key, corresponding to the metadata
     * @throws SQLException if values does not satisfy metadata requirements
     */
    RoutingKey createKey(Object ... a) throws SQLException;
}
