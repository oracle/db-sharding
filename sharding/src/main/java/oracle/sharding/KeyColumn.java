package oracle.sharding;

import java.sql.SQLException;

/**
 * Created by somestuff on 4/1/17.
 */
public interface KeyColumn {
    Object createValue(Object from) throws SQLException;
}
