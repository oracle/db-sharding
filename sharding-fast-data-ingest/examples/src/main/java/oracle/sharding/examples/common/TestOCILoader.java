/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.common;

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

            System.out.println(Parameters.catalogConnectString);
            System.out.flush();

            DirectPathLoadSink sink = new DirectPathLoadSink.Builder(
                    Parameters.catalogConnectString, Parameters.username, Parameters.password)
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
