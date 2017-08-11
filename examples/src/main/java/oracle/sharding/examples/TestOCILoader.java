package oracle.sharding.examples;

import oracle.sharding.tools.DirectPathLoadSink;
import oracle.sharding.tools.SeparatedString;

import java.util.Collections;

/**
 * Test native OCI Direct Path Loader
 */
public class TestOCILoader {
    public static void main(String [] args)
    {
        try {
            Parameters.init(args);

            System.out.println(Parameters.connectionString);
            System.out.flush();

            DirectPathLoadSink sink = new DirectPathLoadSink.Builder(
                    Parameters.connectionString, Parameters.username, Parameters.password)
                    .setTarget(Parameters.schemaName, "LOG", "LOG_P1")
                    .column("CUST_ID", 128)
                    .column("IP_ADDR", 128)
                    .column("HITS", 64).build();

            sink.accept(Collections.singletonList(new SeparatedString("855|29a5:8abc:98d1:a599|658535", '|', 3)));

            sink.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
