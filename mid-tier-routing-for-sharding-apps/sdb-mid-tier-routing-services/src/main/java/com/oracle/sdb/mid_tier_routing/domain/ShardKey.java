/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author bhonnena
 *
 */
public class ShardKey {
	
	private String shardKeyType;
	
	private String shardKeyValue;

	@JsonProperty
	private boolean isSuperShardKey;

	public ShardKey() {
		
	}
	
	public ShardKey(String shardKeyType, String shardKeyValue, boolean isSuperShardKey) {
		super();
		this.shardKeyType = shardKeyType;
		this.shardKeyValue = shardKeyValue;
		this.isSuperShardKey = isSuperShardKey;
	}

	/**
	 * @return the shardKeyType
	 */
	public String getShardKeyType() {
		return shardKeyType;
	}

	/**
	 * @return the shardKeyValue
	 */
	public String getShardKeyValue() {
		return shardKeyValue;
	}

	/**
	 * @return the isSuperShardKey
	 */
	public boolean isSuperShardKey() {
		return isSuperShardKey;
	}

	/**
	 * @param shardKeyType the shardKeyType to set
	 */
	public void setShardKeyType(String shardKeyType) {
		this.shardKeyType = shardKeyType;
	}

	/**
	 * @param shardKeyValue the shardKeyValue to set
	 */
	public void setShardKeyValue(String shardKeyValue) {
		this.shardKeyValue = shardKeyValue;
	}

	/**
	 * @param isSuperShardKey the isSuperShardKey to set
	 */
	public void setSuperShardKey(boolean isSuperShardKey) {
		this.isSuperShardKey = isSuperShardKey;
	}
	

}
