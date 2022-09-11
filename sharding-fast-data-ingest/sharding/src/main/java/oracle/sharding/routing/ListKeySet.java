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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SetOfKeys for List sharding
 */
public class ListKeySet extends SetOfKeys {
    private final Set<RoutingKey> keys = new HashSet<>();

    public ListKeySet(RoutingKey ... keys) {
        this.keys.addAll(Arrays.asList(keys));
    }

    public Set<RoutingKey> getKeys() {
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public boolean contains(RoutingKey routingKey) {
        return keys.contains(routingKey);
    }
}
