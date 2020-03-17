package com.oracle.sdb.mid_tier_routing.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oracle.sdb.mid_tier_routing.dao.interfaces.ISwimLaneDao;
import com.oracle.sdb.mid_tier_routing.domain.SwimLane;
import com.oracle.sdb.mid_tier_routing.service.interfaces.ISwimLaneService;

@Service
public class SwimLaneService implements ISwimLaneService{

	@Autowired
	private ISwimLaneDao swimLaneDao;
	
	@Override
	public void addSwimLane(SwimLane lane) {
		swimLaneDao.addSwimLane(lane);	
	}

	@Override
	public void updateSwimLane(SwimLane lane) {
		swimLaneDao.updateSwimLane(lane);	
	}

	@Override
	public SwimLane fetchSwimLane(String shardName) {
		return swimLaneDao.fetchSwimLane(shardName);
	}

	@Override
	public void deleteSwimLane(String ShardName) {
		swimLaneDao.deleteSwimLane(ShardName);
	}

	
	
}
