/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Wrap query result set into a stream.
 */
public class QueryStream extends Spliterators.AbstractSpliterator<ResultSet> {
    private final ResultSet resultSet;
    private final PreparedStatement statement;

    /**
     * Disable the constructor
     */
    private QueryStream() {
        super(Long.MAX_VALUE, Spliterator.ORDERED);
        throw new RuntimeException("Constructor not available");
    }

    private QueryStream(final PreparedStatement statement) throws SQLException {
        super(Long.MAX_VALUE, Spliterator.ORDERED);
        this.statement = statement;
        this.resultSet = statement.executeQuery();
    }

    @Override
    public boolean tryAdvance(Consumer<? super ResultSet> action) {
        boolean hasNext;

        try {
            hasNext = resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (hasNext) {
            action.accept(resultSet);
        }

        return hasNext;
    }

    public void close() {
        try { resultSet.close(); } catch (SQLException ignore) { /* ignore */ }
        try { statement.close(); } catch (SQLException ignore) { /* ignore */ }
    }

    private Stream<ResultSet> toStream() {
        return StreamSupport.stream(this, false).onClose(this::close);
    }

    /**
     * Execute statement and wrap the result set into a stream.
     *
     * @param statement to execute
     * @return stream wrapped result set
     * @throws SQLException
     */
    public static Stream<ResultSet> create(PreparedStatement statement) throws SQLException
    {
        return new QueryStream(statement).toStream();
    }
}
