/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oracle.sdb.mid_tier_routing.domain.MediaTypes;
import com.oracle.sdb.mid_tier_routing.domain.ShardKey;
import com.oracle.sdb.mid_tier_routing.domain.ShardName;
import com.oracle.sdb.mid_tier_routing.domain.SwimLane;
import com.oracle.sdb.mid_tier_routing.service.interfaces.IShardInfoService;
import com.oracle.sdb.mid_tier_routing.service.interfaces.ISwimLaneService;

import oracle.ucp.routing.oracle.OracleShardRoutingCache;

@RestController
@RequestMapping("/")
public class MidTierRoutingController {

	@Autowired
	ISwimLaneService swimLaneService;
	
	@Autowired
	IShardInfoService shardInfoService;

	OracleShardRoutingCache routingCache;

	private final String user;
	private final String password;
	private final String url;
	private final String serviceName;

	private Logger logger = LoggerFactory.getLogger(MidTierRoutingController.class);

	@Autowired
	public MidTierRoutingController(@Value("${catalog.user}") String user,
			@Value("${catalog.password}") String password, @Value("${catalog.url}") String url,
			@Value("${catalog.svcName:}") String serviceName) {
		this.user = user;
		this.password = password;
		this.url = url;
		this.serviceName = serviceName;

		Properties props = new Properties();
		try {

			props.setProperty("user", this.user);
			props.setProperty("password", this.password);
			props.setProperty("url", this.url);
			if (this.serviceName != null && !this.serviceName.isEmpty()) {
				props.setProperty("serviceName", this.serviceName);
			}

			routingCache = new OracleShardRoutingCache(props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/swimLane", method = RequestMethod.POST, consumes = {
			MediaTypes.VND_ORCL_SDB_MTR_SWIMLANE_V1_JSON })
	public ResponseEntity<Void> addSwimLane(@RequestBody SwimLane swimLane) {
		swimLaneService.addSwimLane(swimLane);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@CrossOrigin
	@RequestMapping(value = "/swimLane/{shardName}", method = RequestMethod.PUT, consumes = {
			MediaTypes.VND_ORCL_SDB_MTR_SWIMLANE_V1_JSON })
	public ResponseEntity<Void> updateSwimLane(@PathVariable String shardName, @RequestBody SwimLane swimLane) {
		swimLaneService.updateSwimLane(swimLane);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@CrossOrigin
	@RequestMapping(value = "/swimLane/{shardName}", method = RequestMethod.GET, produces = {
			MediaTypes.VND_ORCL_SDB_MTR_SWIMLANE_V1_JSON })
	public ResponseEntity<SwimLane> retrieveSwimLane(@PathVariable String shardName) {
		SwimLane lane = swimLaneService.fetchSwimLane(shardName);

		if (lane == null) {
			return new ResponseEntity<SwimLane>(lane, HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<SwimLane>(lane, HttpStatus.OK);
		}
	}

	@CrossOrigin
	@RequestMapping(value = "/swimLane/{shardName}", method = RequestMethod.DELETE, consumes = {
			MediaTypes.VND_ORCL_SDB_MTR_SWIMLANE_V1_JSON })
	public ResponseEntity<Void> deleteSwimLane(@PathVariable String shardName) {
		swimLaneService.deleteSwimLane(shardName);
		return new ResponseEntity<Void>(HttpStatus.OK);
	}

	@CrossOrigin
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/shardDetails", method = RequestMethod.POST, consumes = {
			MediaTypes.VND_ORCL_SDB_MTR_SK_DATATYPE_MIXED_V1_JSON })
	public ResponseEntity<List<ShardName>> getShardNameForMixedShardKeyTypes(@RequestBody List<ShardKey> shardKeys) {
		
		List<String> shardNameList = shardInfoService.getShardName(shardKeys, routingCache);
		List<ShardName> shdNamesList = Collections.emptyList();
	
		if (shardNameList == null || shardNameList.isEmpty()) {
			return new ResponseEntity<List<ShardName>>(shdNamesList, HttpStatus.NOT_FOUND);
		} else {
			shdNamesList = new ArrayList<>();
			for(String s : shardNameList) {
				ShardName shdNameObj = new ShardName();
				shdNameObj.setShardName(s);
				shdNamesList.add(shdNameObj);
			}

			return new ResponseEntity<List<ShardName>>(shdNamesList, HttpStatus.OK);
		}
	}

	@CrossOrigin
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/shardDetails/swimLane", method = RequestMethod.POST, consumes = {
			MediaTypes.VND_ORCL_SDB_MTR_SK_DATATYPE_MIXED_SWIMLANE_V1_JSON })
	public ResponseEntity<List<SwimLane>> getSwimLaneDetailsForMixedShardKeyTypes(@RequestBody List<ShardKey> shardKeys) {
		
		List<String> shardNameList = shardInfoService.getShardName(shardKeys, routingCache);	
		
		List<SwimLane> swimLaneList = Collections.emptyList();
		
		for(String s : shardNameList) {
			SwimLane lane = swimLaneService.fetchSwimLane(s);
			swimLaneList = new ArrayList<>();
			if (lane != null) {
				swimLaneList.add(lane);
			}
		}
		
		if (swimLaneList.isEmpty()) {
			return new ResponseEntity<List<SwimLane>>(swimLaneList, HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<List<SwimLane>>(swimLaneList, HttpStatus.OK);
		}

	}

}