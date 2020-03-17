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
 * Exception for Shard Configuration
 */
public class ShardConfigurationException extends SQLException {
    public ShardConfigurationException(String s) {
        super(s);
    }
}
