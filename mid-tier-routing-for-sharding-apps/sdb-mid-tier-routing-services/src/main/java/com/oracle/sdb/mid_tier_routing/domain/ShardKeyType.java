/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing.domain;

import oracle.jdbc.OracleType;
import java.math.BigDecimal;

/**
 * @author bhonnena
 *
 */
public enum ShardKeyType{
	
	NUMBER("NUMBER", OracleType.NUMBER, BigDecimal.class),
	VARCHAR2("VARCHAR2", OracleType.VARCHAR2, String.class),
	CHAR("CHAR", OracleType.CHAR, String.class),
	NVARCHAR("NVARCHAR", OracleType.NVARCHAR, String.class),
	FLOAT("FLOAT", OracleType.FLOAT, Double.class),
	RAW("RAW", OracleType.RAW, Byte.class),
	DATE("DATE", OracleType.DATE, java.sql.Timestamp.class),
	TIMESTAMP("TIMESTAMP", OracleType.TIMESTAMP, java.sql.Timestamp.class),
	TIMESTAMP_WITH_LOCAL_TIME_ZONE("TIMESTAMP_WITH_LOCAL_TIME_ZONE", OracleType.TIMESTAMP_WITH_LOCAL_TIME_ZONE, java.sql.Timestamp.class);
	
	
	private String name; 
	private OracleType type;
	private Class clazz;
		
	ShardKeyType(String name, OracleType type, Class clazz){
		this.name = name;
		this.type = type;
		this.clazz = clazz;		
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @return the type
	 */
	public OracleType getType() {
		return type;
	}


	/**
	 * @return the class
	 */
	public Class getClazz() {
		return clazz;
	}
	
}
