/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

import oracle.sharding.ShardConfigurationException;
import oracle.sharding.routing.KeyColumn;
import oracle.sharding.RoutingKey;
import oracle.sql.CHAR;
import oracle.sql.CharacterSet;
import oracle.sql.NUMBER;
import oracle.sql.RAW;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Oracle type routing key wrappers
 */
public abstract class OracleKeyColumn implements KeyColumn {
    private final int dataType;

    static class OracleNumberWrapper extends NUMBER implements RoutingKey
    {
        BigDecimal decimalRepresentation;

        public OracleNumberWrapper(byte[] from) {
            super(from);
        }

        @Override
        public BigDecimal bigDecimalValue() throws SQLException {
            if (decimalRepresentation == null) {
                decimalRepresentation = super.bigDecimalValue();
            }

            return super.bigDecimalValue();
        }

        public OracleNumberWrapper(NUMBER from) {
            super(from.toBytes());
        }

        @Override
        public int hashCode() {
            return OracleKggHash.hash(this.getBytes(), 0);
        }

        @Override
        public int compareTo(RoutingKey o) {
            try {
                return this.bigDecimalValue().compareTo(((OracleNumberWrapper) o).bigDecimalValue());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Number extends OracleKeyColumn
    {
        public Number(int dataType) {
            super(dataType);
        }

        @Override
        public RoutingKey createValue(Object from) throws SQLException {
            if (from instanceof NUMBER) {
                return new OracleNumberWrapper((NUMBER) from);
            } else if (from instanceof byte[]) {
                return new OracleNumberWrapper((byte[]) from);
            }

            return new OracleNumberWrapper(new NUMBER(from));
        }
    }

    private static int memcmp(byte b1[], byte b2[])
    {
        int sz1 = b1.length;
        int sz2 = b2.length;
        int sz  = Math.min(sz1, sz2);

        for (int i = 0; i < sz; i++) {
            if (b1[i] != b2[i]) {
                return Byte.toUnsignedInt(b1[i]) - Byte.toUnsignedInt(b2[i]);
            }
            --sz1; --sz2;
        }

        if (sz1 > 0) return 1;
        if (sz2 > 0) return 1;

        return 0;
    }

    static class OracleComparableBinary implements RoutingKey
    {
        private final byte[] data; /* Data in database internal encoding */

        OracleComparableBinary(byte[] from) {
            data = from;
        }

        @Override
        public int hashCode() {
            return OracleKggHash.hash(data, 0);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof OracleComparableBinary && Arrays.equals(data, ((OracleComparableBinary) obj).data);
        }

        @Override
        public int compareTo(RoutingKey o) {
            return memcmp(data, ((OracleComparableBinary) o).data);
        }
    }

    public static class VariableBinaryString extends OracleKeyColumn
    {
        private final int maxSize;

        public VariableBinaryString(int dataType, int maxSize) {
            super(dataType);
            this.maxSize = maxSize;
        }

        @Override
        public RoutingKey createValue(Object from) throws SQLException {
            if (from instanceof RAW) {
                return new OracleComparableBinary(((RAW) from).getBytes());
            } else if (from instanceof byte[]) {
                return new OracleComparableBinary((byte[]) from);
            } else if (from instanceof String) {
                return new OracleComparableBinary(((String) from).getBytes());
            }

            throw new SQLException("TODO");
        }
    }

    public static class VariableCharacter extends VariableBinaryString
    {
        final CharacterSet charSet;

        VariableCharacter(int dataType, int maxSize, int charSet) {
            super(dataType, maxSize);
            this.charSet = CharacterSet.make(charSet);
        }

        @Override
        public RoutingKey createValue(Object from) throws SQLException {
            if (from instanceof CHAR) {
                from = ((CHAR) from).getString();
            } else if (from instanceof CharSequence) {
                from = from.toString();
            }

            if (from instanceof String) {
                return new OracleComparableBinary(this.charSet.convert((String) from));
            }

            return super.createValue(from);
        }
    }

    public static class FixedCharacter extends VariableCharacter
    {
        private final String paddedFormat;

        public FixedCharacter(int dataType, int size, int charSet) {
            super(dataType, size, charSet);
            paddedFormat = "%1$-" + size + "s";
        }

        @Override
        public RoutingKey createValue(Object from) throws SQLException {
            if (from instanceof CHAR) {
                from = ((CHAR) from).getString();
            }

            if (from instanceof String) {
                return new OracleComparableBinary(this.charSet.convert(String.format(paddedFormat, (String) from)));
            }

            throw new SQLException("TODO");
        }
    }

    public static final int DTY_VARCHAR = 1;
    public static final int DTY_NUMBER  = 2;
    public static final int DTY_RAW     = 23;
    public static final int DTY_CHAR    = 96;

    public static OracleKeyColumn createOracleKeyColumn(int dataType, int charSet, int size)
            throws ShardConfigurationException
    {
        switch (dataType) {
            case DTY_VARCHAR :
                return new VariableCharacter(dataType, size, charSet);
            case DTY_NUMBER :
                return new Number(dataType);
            case DTY_RAW :
                return new VariableBinaryString(dataType, size);
            case DTY_CHAR :
                return new FixedCharacter(dataType, size, charSet);
/*
            case 12:
            case 180:
            case 231:
*/
            default : throw new ShardConfigurationException(
                    String.format("Type %d is not supported by implementation", dataType));
        }
    }

    public OracleKeyColumn(int dataType) {
        this.dataType = dataType;
    }
}
