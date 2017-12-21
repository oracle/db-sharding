/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;
import oracle.sharding.SetOfKeys;
import oracle.sql.NUMBER;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Key set implementation for hash sharding
 */
public class HashKeySet extends SetOfKeys implements Comparable<HashKeySet> {
    final public long lowerClosed;
    final public long upperOpen;

    public HashKeySet(long lowerClosed, long upperOpen) {
        this.lowerClosed = lowerClosed;
        this.upperOpen = upperOpen;
    }

    public int compareToLong(long y) {
        return Long.compare(lowerClosed, y);
    }

    public static HashKeySet create(byte[] lowerClosed, byte[] upperOpen)
            throws SQLException
    {
        long loBound, hiBound;

        if (lowerClosed == null || lowerClosed.length == 0 || lowerClosed[0] == (byte) 0xff) {
            loBound = -1;
        } else {
            loBound = new NUMBER(Arrays.copyOfRange(lowerClosed, 1, lowerClosed.length)).longValue();
        }

        if (upperOpen == null || upperOpen.length == 0 || upperOpen[0] == (byte) 0xff) {
            hiBound = Long.MAX_VALUE;
        } else {
            hiBound = new NUMBER(Arrays.copyOfRange(upperOpen, 1, upperOpen.length)).longValue();
        }

        return new HashKeySet(loBound, hiBound);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(lowerClosed) ^ Long.hashCode(upperOpen);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HashKeySet
                && lowerClosed == ((HashKeySet) obj).lowerClosed
                && upperOpen == ((HashKeySet) obj).upperOpen;
    }

    public boolean contains(long value) {
        return value >= lowerClosed && value < upperOpen;
    }

    public boolean contains(RoutingKey value) {
        return contains(Integer.toUnsignedLong(value.hashCode()));
    }

/*
    public String getConnectionKey() {
        return "(SHARDING_KEY_B64=1 4," + Base64.getEncoder().encodeToString(new NUMBER(lowerClosed).toBytes());
    }
*/

    @Override
    public int compareTo(HashKeySet o) {
        int cmp = Long.compare(lowerClosed, o.lowerClosed);
        return (cmp != 0) ? cmp : -Long.compare(upperOpen, o.upperOpen);
    }
}
