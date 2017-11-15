/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import java.sql.SQLException;

/**
 * Created by itaranov on 4/6/17.
 */
public class UnexpectedChunkConfigurationException extends SQLException {
    public UnexpectedChunkConfigurationException(String s) {
        super(s);
    }
}
