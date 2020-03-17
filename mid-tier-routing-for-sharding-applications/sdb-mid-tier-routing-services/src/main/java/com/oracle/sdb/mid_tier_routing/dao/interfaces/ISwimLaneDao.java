/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.dao.interfaces;

import com.oracle.sdb.mid_tier_routing.domain.SwimLane;

/**
 * @author bhonnena
 *
 */

public interface ISwimLaneDao {
	/**
	 * 
	 * @param lane
	 */
	void addSwimLane(SwimLane lane);
	
	/**
	 * 
	 * @param lane
	 */
	void updateSwimLane(SwimLane lane);
	
	/**
	 * 
	 * @param shardName
	 * @return
	 */
	SwimLane fetchSwimLane(String shardName);
	
	/**
	 * 
	 * @param ShardName
	 */
	void deleteSwimLane(String ShardName);
}
