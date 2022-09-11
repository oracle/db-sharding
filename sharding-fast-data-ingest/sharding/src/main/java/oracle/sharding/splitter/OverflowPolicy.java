/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.splitter;

/**
 * Overflow policy
 */
public class OverflowPolicy<ItemT> {
    public static OverflowPolicy CREATE_SINK   = new OverflowPolicy();
    public static OverflowPolicy WAIT_FOR_SINK = new OverflowPolicy();

    private OverflowPolicy() {
    }

    public Object apply(Object container, ItemT batch) {
        return this;
    }
}
