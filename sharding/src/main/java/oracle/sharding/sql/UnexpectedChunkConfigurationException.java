package oracle.sharding.sql;

import java.sql.SQLException;

/**
 * Created by somestuff on 4/6/17.
 */
public class UnexpectedChunkConfigurationException extends SQLException {
    public UnexpectedChunkConfigurationException(String s) {
        super(s);
    }
}
