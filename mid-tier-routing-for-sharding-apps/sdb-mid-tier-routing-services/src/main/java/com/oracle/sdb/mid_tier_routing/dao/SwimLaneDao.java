/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.dao;

import javax.annotation.PostConstruct;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.oracle.sdb.mid_tier_routing.dao.interfaces.ISwimLaneDao;
import com.oracle.sdb.mid_tier_routing.domain.Constants;
import com.oracle.sdb.mid_tier_routing.domain.SwimLane;

@Repository
public class SwimLaneDao implements ISwimLaneDao {

	@Autowired
	private RedisTemplate<String, SwimLane> redisTemplate;

	@Value("${cacheType}")
	private String cacheType;

	private Cache<String, SwimLane> localCache;

	@PostConstruct
	public void init() {
		localCache = new Cache2kBuilder<String, SwimLane>() {
		}.name("shardKeyToSwimLane").eternal(true).entryCapacity(Constants.CACHE_SIZE).build();
	}

	@Override
	public void addSwimLane(SwimLane lane) {
		if (cacheType.equalsIgnoreCase(Constants.REDIS_CACHE_TYPE)) {
			redisTemplate.opsForValue().set(lane.getShardName(), lane);
		} else {
			localCache.put(lane.getShardName(), lane);
		}
	}

	@Override
	public void updateSwimLane(SwimLane lane) {
		if (cacheType.equalsIgnoreCase(Constants.REDIS_CACHE_TYPE)) {
			redisTemplate.opsForValue().set(lane.getShardName(), lane);
		} else {
			localCache.put(lane.getShardName(), lane);
		}
	}

	@Override
	public SwimLane fetchSwimLane(String shardName) {
		if (cacheType.equalsIgnoreCase(Constants.REDIS_CACHE_TYPE)) {
			return redisTemplate.opsForValue().get(shardName);
		} else {
			return localCache.get(shardName);
		}

	}

	@Override
	public void deleteSwimLane(String shardName) {
		if (cacheType.equalsIgnoreCase(Constants.REDIS_CACHE_TYPE)) {
			redisTemplate.delete(shardName);
		} else {
			localCache.remove(shardName);
		}
	}

}