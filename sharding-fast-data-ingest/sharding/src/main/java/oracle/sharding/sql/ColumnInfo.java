/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.sql;

import oracle.sharding.ShardConfigurationException;
import oracle.sharding.details.OracleKeyColumn;

import java.io.Serializable;

/**
 * Sharding column information from LOCAL_CHUNK_COLUMNS
 */
public class ColumnInfo implements Serializable {
    final int dty, charSet, size;
    final int level, number;

    public ColumnInfo(int dty, int charSet, int size, int level, int number) {
        this.dty = dty;
        this.charSet = charSet;
        this.size = size;
        this.level = level;
        this.number = number;
    }

    public OracleKeyColumn toOracleColumn() throws ShardConfigurationException {
        return OracleKeyColumn.createOracleKeyColumn(dty, charSet, size);
    }
}
