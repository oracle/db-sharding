/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.routing;

import oracle.sharding.RoutingKey;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Simple implementation for multi-column key
 */
public class MultiColumnKey implements RoutingKey {
    final RoutingKey[] values;

    private MultiColumnKey(int columns) {
        this.values = new RoutingKey[columns];
    }

    public MultiColumnKey(RoutingKey[] values, int begin) {
        this.values = Arrays.copyOfRange(values, begin, begin + values.length);
    }

    public MultiColumnKey(SimpleKeyMetadata metadata) {
        this(metadata.getColumnCount());
    }

    public MultiColumnKey(SimpleKeyMetadata metadata, RoutingKey[] values, int begin) {
        this.values = Arrays.copyOfRange(values, begin, begin + metadata.getColumnCount());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MultiColumnKey && Arrays.equals(values, ((MultiColumnKey) obj).values);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new MultiColumnKey(values, 0);
    }

    @Override
    public int hashCode() {
        int hash = 0;

        for (Object part : values) {
            hash += part.hashCode();
        }

        return hash;
    }

    private static Comparator<Comparable> nullSafeComparator = Comparator
            .nullsFirst(Comparable::compareTo);

    @Override
    public int compareTo(RoutingKey other) {
        final MultiColumnKey a = this;
        final MultiColumnKey b = (MultiColumnKey) other;

        int n = Math.min(a.values.length, b.values.length);

        for (int i = 0; i < n; ++i) {
            final int cmp = nullSafeComparator.compare(a.values[i], b.values[i]);

            if (cmp != 0) {
                return cmp;
            }
        }

        return Integer.compare(a.values.length, b.values.length);
    }

}
