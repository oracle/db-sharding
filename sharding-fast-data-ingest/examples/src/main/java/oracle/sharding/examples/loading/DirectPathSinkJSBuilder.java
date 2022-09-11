/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.loading;

import oracle.sharding.tools.DirectPathLoadSink;
import oracle.sharding.tools.OCIDirectPath;
import oracle.util.settings.JSWrapper;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * TODO:
 */
class DirectPathSinkJSBuilder {
    private final OCIDirectPath dpl;
    private final int columnCount;

    private static IllegalArgumentException fieldRequired(String value, String name) {
        return new IllegalArgumentException(value + " ('" + name + "') must be specified for DirectPathLoad sink");
    }

    public DirectPathSinkJSBuilder(JSWrapper parameters) throws SQLException {
        dpl = new DirectPathLoadSink.Builder(
                parameters.get("catalogConnectString")
                        .orElseThrow(() -> fieldRequired("Connection string", "catalogConnectString"))
                        .asString()
                , parameters.get("user")
                .orElseThrow(() -> fieldRequired("Username", "user"))
                .asString()
                , parameters.get("password")
                .orElseThrow(() -> fieldRequired("Password", "password"))
                .asString()).setTarget(
                parameters.get("schema")
                        .orElseThrow(() -> fieldRequired("Schema", "schema"))
                        .asString()
                , parameters.get("table")
                        .orElseThrow(() -> fieldRequired("Table", "table"))
                        .asString()
                , parameters.get("partition")
                        .orElseThrow(() -> fieldRequired("Partition or subpartition", "partition"))
                        .asString()).getDpl();

        Collection<Object> columnDefinitions = parameters.get("columns")
                .orElseThrow(() -> fieldRequired("Column definition", "column"))
                .asCollection();

        this.columnCount = columnDefinitions.size();

        for (Map.Entry<String, Object> entry : parameters.asMap().entrySet()) {
            if (entry.getKey().startsWith("OCI")) {
                dpl.setAttribute(entry.getKey(), JSWrapper.of(entry.getValue()).asString());
            }
        }

        dpl.begin();
    }
}
