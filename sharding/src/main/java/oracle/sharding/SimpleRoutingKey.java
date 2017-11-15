/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by itaranov on 4/1/17.
 */
public class SimpleRoutingKey extends RoutingKey {
    final Object[] values;

    public SimpleRoutingKey(SimpleKeyMetadata metadata) {
        super(metadata);
        this.values = new Object[metadata.getColumnCount()];
    }

    public SimpleRoutingKey(SimpleKeyMetadata metadata, Object[] values, int begin) {
        super(metadata);
        this.values = Arrays.copyOfRange(values, begin, begin + metadata.getColumnCount());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SimpleRoutingKey && Arrays.equals(values, ((SimpleRoutingKey) obj).values);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new SimpleRoutingKey((SimpleKeyMetadata) metadata, values, 0);
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
        final SimpleRoutingKey a = this;
        final SimpleRoutingKey b = (SimpleRoutingKey) other;

        int n = Math.min(a.values.length, b.values.length);

        for (int i = 0; i < n; ++i) {
            final int cmp = nullSafeComparator.compare((Comparable) a.values[i], (Comparable) b.values[i]);

            if (cmp != 0) {
                return cmp;
            }
        }

        return Integer.compare(a.values.length, b.values.length);
    }

}
