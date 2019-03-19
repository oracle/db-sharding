/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package com.oracle.sdb.mid_tier_routing;

import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.oracle.sdb.mid_tier_routing.domain.Constants;
import com.oracle.sdb.mid_tier_routing.domain.SwimLane;

@SpringBootApplication
public class MidTierRoutingServices extends SpringBootServletInitializer {

	@Value("${cacheHost}")
	private String host;
	@Value("${cachePort}")
	private Integer port;
	@Value("${cacheType}")
	private String cacheType;

	private Logger logger = LoggerFactory.getLogger(MidTierRoutingServices.class);
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MidTierRoutingServices.class);
	}

	public static void main(String[] args) throws NamingException {
		SpringApplication.run(MidTierRoutingServices.class, args);
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {

		logger.debug(" Cache Host :  " + host);
		logger.debug(" Cache type : " + cacheType);

		if (!cacheType.equalsIgnoreCase(Constants.REDIS_CACHE_TYPE)) {
			host = Constants.CACHE_LOCAL_HOST_NAME;
			port = Constants.CACHE_LOCAL_PORT;
		}

		JedisConnectionFactory factory = new JedisConnectionFactory();
		factory.setHostName(host);
		factory.setPort(port);

		logger.debug(
				" RedisConnectionFactory details : host=" + factory.getHostName() + "port=" + factory.getPort());
		return factory;
	}

	@Bean
	public RedisTemplate<String, SwimLane> redisTemplate(RedisConnectionFactory cf) {

		RedisTemplate<String, SwimLane> redis = new RedisTemplate<String, SwimLane>();
		redis.setConnectionFactory(cf);
		return redis;
	}
}