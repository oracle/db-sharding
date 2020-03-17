/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

import java.io.Serializable;

/**
 * Base class for representing a set of routing keys
 */
public abstract class SetOfKeys implements Serializable {
    public abstract boolean contains(RoutingKey routingKey);
}
