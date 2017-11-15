/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

/**
 * Created by itaranov on 4/5/17.
 */
public abstract class RoutingKey implements Comparable<RoutingKey> {
    protected final RoutingMetadata metadata;

    public RoutingKey(RoutingMetadata metadata) {
        this.metadata = metadata;
    }
}
