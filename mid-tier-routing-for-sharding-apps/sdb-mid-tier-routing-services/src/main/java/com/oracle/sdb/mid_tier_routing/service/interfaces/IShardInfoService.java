package com.oracle.sdb.mid_tier_routing.service.interfaces;

import java.util.List;

import com.oracle.sdb.mid_tier_routing.domain.ShardKey;

import oracle.ucp.routing.oracle.OracleShardRoutingCache;

public interface IShardInfoService {
	

	/**
	 * getShardName
	 * 
	 * @param shardKeys
	 * @param routingCache
	 * @return List of ShardName
	 */
	List<String> getShardName(List<ShardKey> shardKeys, OracleShardRoutingCache routingCache);
	
}
