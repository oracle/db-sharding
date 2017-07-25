package oracle.sharding;

import java.sql.SQLException;

/**
 * Created by somestuff on 4/5/17.
 */
public class ConfigurationNotSupportedException extends SQLException {
    public ConfigurationNotSupportedException(String s) {
        super(s);
    }
}
