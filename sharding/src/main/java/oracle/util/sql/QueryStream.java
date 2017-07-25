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
 * Created by somestuff on 6/30/17.
 */
public class QueryStream {
    public static class ResultSetSpliterator extends Spliterators.AbstractSpliterator<ResultSet> {
        private final ResultSet resultSet;

        public ResultSetSpliterator(final ResultSet resultSet) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            this.resultSet = resultSet;
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

        public void close() throws SQLException {
            resultSet.close();
        }
    }

    public static Stream<ResultSet> create(PreparedStatement statement) throws SQLException
    {
        ResultSetSpliterator splt = new ResultSetSpliterator(statement.executeQuery());

        return StreamSupport.stream(splt, false).onClose(() -> {
            try { splt.close(); } catch (SQLException ignore) { /* ignore */ }
            try { statement.close(); } catch (SQLException ignore) { /* ignore */ }
        });
    }
}
