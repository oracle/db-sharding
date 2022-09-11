package com.oracle.sdb.mid_tier_routing.service.interfaces;

import com.oracle.sdb.mid_tier_routing.domain.SwimLane;

public interface ISwimLaneService {

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
