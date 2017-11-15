/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding;

/**
 * Created by itaranov on 4/1/17.
 */
public enum ShardBy {
    HASH(1), RANGE(2), LIST(4), NONE(0);

    int id;
    ShardBy(int aid) { id = aid; }
}
