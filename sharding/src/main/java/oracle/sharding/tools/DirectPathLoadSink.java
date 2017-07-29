package oracle.sharding.tools;

import oracle.util.function.ConsumerWithError;
import oracle.util.settings.JSWrapper;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by somestuff on 7/21/17.
 */
public class DirectPathLoadSink implements AutoCloseable, ConsumerWithError<List<SeparatedString>, SQLException> {
    private final OCIDirectPath dpl;
    private final int columnCount;

    private static IllegalArgumentException fieldRequired(String value, String name) {
        return new IllegalArgumentException(value + " ('" + name + "') must be specified for DirectPathLoad sink");
    }

    public DirectPathLoadSink(JSWrapper parameters) throws SQLException {
        dpl = new OCIDirectPath(
            parameters.get("connectionString")
                .orElseThrow(() -> fieldRequired("Connection string", "connectionString"))
                .asString()
            , parameters.get("user")
                .orElseThrow(() -> fieldRequired("Username", "user"))
                .asString()
            , parameters.get("password")
                .orElseThrow(() -> fieldRequired("Password", "password"))
                .asString().getBytes());

        dpl.setTarget(
                parameters.get("schema")
                        .orElseThrow(() -> fieldRequired("Schema", "schema"))
                        .asString()
                , parameters.get("table")
                        .orElseThrow(() -> fieldRequired("Table", "table"))
                        .asString()
                , parameters.get("partition")
                        .orElseThrow(() -> fieldRequired("Partition or subpartition", "partition"))
                        .asString());

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

    @Override
    public void close() throws Exception {
        dpl.finish();
        dpl.close();
    }

    @Override
    public void accept(List<SeparatedString> separatedStrings) throws SQLException {
        for (SeparatedString s : separatedStrings) {
            dpl.setData(StandardCharsets.UTF_8.encode(s.toCharSequence()).array());

            for (int i = 0; i < 9; ++i) {
                dpl.setValue(i, s.getOffset(i), s.getLength(i));
            }

            dpl.nextRow();
        }

        System.out.println("Loaded");
        System.out.flush();
    }
}
