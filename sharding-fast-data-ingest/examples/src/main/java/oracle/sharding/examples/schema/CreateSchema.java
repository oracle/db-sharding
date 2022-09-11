/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding.examples.schema;

import oracle.sharding.examples.common.Parameters;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create schema for log table examples
 */
public class CreateSchema {
    private void run() throws SQLException {
        try (Connection connection = Parameters.getCatalogConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create tablespace set deflts using\n" +
                    "  template (datafile size 16m autoextend on next 8m maxsize unlimited)");
                statement.execute(DemoLogEntry.getCreateTable());
            }
        }
    }

    public static void main(String [] args)
    {
        try {
            Parameters.init(args);
            new CreateSchema().run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
