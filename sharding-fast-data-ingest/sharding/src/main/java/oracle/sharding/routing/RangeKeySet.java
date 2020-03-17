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

/**
 * SetOfKeys for Range sharding
 */
public class RangeKeySet extends SetOfKeys implements Comparable<RangeKeySet>  {
    private final RoutingKey lowerClosed;
    private final RoutingKey upperOpen;

    public RangeKeySet(RoutingKey lowerClosed, RoutingKey upperOpen) {
        this.lowerClosed = lowerClosed;
        this.upperOpen = upperOpen;
    }

    public RoutingKey getLower() {
        return lowerClosed;
    }

    public RoutingKey getUpper() {
        return upperOpen;
    }

    @Override
    public int compareTo(RangeKeySet other) {
        int cmp = lowerClosed.compareTo(other.lowerClosed);
        return cmp == 0 ? upperOpen.compareTo(other.upperOpen) : cmp;
    }

    @Override
    public boolean contains(RoutingKey x) {
        return x.compareTo(lowerClosed) >= 0 && x.compareTo(upperOpen) < 0;
    }
}
