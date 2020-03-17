/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.tools;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * OCI direct path consumer
 */
public class DirectPathLoadSink implements Consumer<List<SeparatedString>>, AutoCloseable {
    private final OCIDirectPath dpl;
    private int columnCount;
    private boolean ready = false;

    protected DirectPathLoadSink(OCIDirectPath dpl, int columnCount) {
        this.dpl = dpl;
        this.columnCount = columnCount;
    }

    public static class Builder {
        private final OCIDirectPath dpl;
        private int columnCount = 0;

        public int getColumnCount() {
            return columnCount;
        }

        public OCIDirectPath getDpl() {
            return dpl;
        }

        private final static class ColumnDef {
            private final String name;
            private final int value;

            public ColumnDef(String name, int value) {
                this.name = name;
                this.value = value;
            }
        }

        public Builder(String connectionString, String username, String password) {
            dpl = new OCIDirectPath(connectionString, username, password.getBytes());
        }

        public Builder setTarget(String schema, String table, String partition) {
            dpl.setTarget(schema, table, partition);
            return this;
        }

        public Builder column(String name, int len) {
            dpl.addColumnDefinition(name, len);
            ++columnCount;
            return this;
        }

        public Builder property(String key, String value) {
            dpl.setAttribute(key, value);
            return this;
        }

        public DirectPathLoadSink build() {
            return new DirectPathLoadSink(dpl, columnCount);
        }
    }

    @Override
    public void close() throws Exception {
        dpl.finish();
        dpl.close();
    }

    @Override
    public void accept(List<SeparatedString> separatedStrings) {
        if (!ready) {
            dpl.begin();
            ready = true;
        }

        for (SeparatedString s : separatedStrings) {
            dpl.setData(StandardCharsets.UTF_8.encode(s.toCharSequence()).array());

            for (int i = 0; i < columnCount; ++i) {
                dpl.setValue(i, s.getOffset(i), s.getLength(i));
            }

            dpl.nextRow();
        }
    }
}
