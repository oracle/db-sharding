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
 * Created by itaranov on 4/1/17.
 */
public interface KeyColumn {
    Object createValue(Object from) throws SQLException;
}
