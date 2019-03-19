/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.domain;

import java.io.Serializable;


/**
 * @author bhonnena
 *
 */

public class SwimLane implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	//@Id
	private String shardName;
	
	private String swimLaneName;	
	private String swimLaneURL;
	
	public SwimLane() {};
	
	/**
	 * @return the shardName
	 */
	public String getShardName() {
		return shardName;
	}
	/**
	 * @return the swimLaneName
	 */
	public String getSwimLaneName() {
		return swimLaneName;
	}
	/**
	 * @return the swimLaneURL
	 */
	public String getSwimLaneURL() {
		return swimLaneURL;
	}
	/**
	 * @param shardName the shardName to set
	 */
	public void setShardName(String shardName) {
		this.shardName = shardName;
	}
	/**
	 * @param swimLaneName the swimLaneName to set
	 */
	public void setSwimLaneName(String swimLaneName) {
		this.swimLaneName = swimLaneName;
	}
	/**
	 * @param swimLaneURL the swimLaneURL to set
	 */
	public void setSwimLaneURL(String swimLaneURL) {
		this.swimLaneURL = swimLaneURL;
	}
	
}
