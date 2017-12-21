/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A DML consumer
 * @param <T>
 */
public class StatementSink<T> implements Consumer<List<T>>, AutoCloseable {
    private boolean closeConnection = true;
    private PreparedStatement statement;
    private final Supplier<PreparedStatement> statementSupplier;
    private final BiConsumer<T, PreparedStatement> bindFunction;

    public StatementSink(Supplier<PreparedStatement> statementSupplier, BiConsumer<T, PreparedStatement> bindFunction)
    {
        this.statementSupplier = statementSupplier;
        this.bindFunction = bindFunction;
    }

    public StatementSink<T> setCloseConnection(boolean value) {
        closeConnection = value;
        return this;
    }

    @Override
    public void close() throws Exception {
        Connection connection = statement.getConnection();

        try { statement.close(); } catch (SQLException ignore) { };

        if (closeConnection) {
            connection.close();
        }
    }

    @Override
    public void accept(List<T> ts) {
        if (statement == null) {
            statement = statementSupplier.get();
        }

        try {
            for (T obj : ts) {
                bindFunction.accept(obj, statement);
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
