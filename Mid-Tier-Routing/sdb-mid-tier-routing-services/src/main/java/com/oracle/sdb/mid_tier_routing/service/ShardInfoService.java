/**
 * 
 */
package com.oracle.sdb.mid_tier_routing.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.oracle.sdb.mid_tier_routing.domain.ShardKey;
import com.oracle.sdb.mid_tier_routing.domain.ShardKeyType;
import com.oracle.sdb.mid_tier_routing.service.interfaces.IShardInfoService;

import oracle.jdbc.OracleShardingKey;
import oracle.jdbc.OracleShardingKeyBuilder;
import oracle.jdbc.pool.OracleDataSource;
import oracle.ucp.routing.ShardInfo;
import oracle.ucp.routing.oracle.OracleShardRoutingCache;

/**
 * @author bhonnena
 *
 */

@Service
public class ShardInfoService implements IShardInfoService {
	
	private Logger logger = LoggerFactory.getLogger(ShardInfoService.class);
	
	@Override
	public List<String> getShardName(List<ShardKey> shardKeys, OracleShardRoutingCache routingCache) {
		
		logger.debug("Shard Keys List");
		Consumer<ShardKey> shKeyList = (ShardKey sk) -> logger.debug("value:" + sk.getShardKeyValue() + ", Type:"
				+ sk.getShardKeyType() + ", isSuperShardKey : " + sk.isSuperShardKey());
		shardKeys.forEach(shKeyList);

		Predicate<ShardKey> superShardKeysPred = shardKey -> Boolean
				.valueOf(shardKey.isSuperShardKey()) == Boolean.TRUE;

		Predicate<ShardKey> shardKeysPred = shardKey -> Boolean.valueOf(shardKey.isSuperShardKey()) == Boolean.FALSE;

		List<ShardKey> superShardKeyList = shardKeys.stream().filter(superShardKeysPred)
				.collect(Collectors.<ShardKey>toList());

		List<ShardKey> shardKeyList = shardKeys.stream().filter(shardKeysPred).collect(Collectors.<ShardKey>toList());

		logger.debug("Filtered Shard Key List");
		Consumer<ShardKey> style = (ShardKey sk) -> logger
				.debug("value:" + sk.getShardKeyValue() + ", Type:" + sk.getShardKeyType());
		shardKeyList.forEach(style);

		logger.debug("Filtered Super Shard Key List");
		superShardKeyList.forEach(style);

		OracleShardingKey shardingKey = null;
		OracleShardingKey superShardingKey = null;

		try {

			OracleShardingKeyBuilder shardKeyBuilder = new OracleDataSource().createShardingKeyBuilder();

			for (ShardKey sk : shardKeyList) {
				if (sk.getShardKeyType() != null) {
					ShardKeyType skType = ShardKeyType.valueOf(sk.getShardKeyType().toUpperCase());

					if (sk.getShardKeyType().toUpperCase().equals(ShardKeyType.DATE.toString())
							|| sk.getShardKeyType().toUpperCase().equals(ShardKeyType.TIMESTAMP.toString())
							|| sk.getShardKeyType().toUpperCase()
									.equals(ShardKeyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE.toString())) {
						Timestamp ts = Timestamp.valueOf(sk.getShardKeyValue());
						shardKeyBuilder.subkey(ts, skType.getType());
					} else if (sk.getShardKeyType().toUpperCase().equals(ShardKeyType.RAW.toString())) {
						byte[] bytes = sk.getShardKeyValue().getBytes();
						shardKeyBuilder.subkey(bytes, skType.getType());
					} else {
						Constructor<?> ctor = skType.getClazz().getConstructor(String.class);
						shardKeyBuilder.subkey(ctor.newInstance(sk.getShardKeyValue()), skType.getType());
					}
				}
			}

			shardingKey = shardKeyBuilder.build();

			if (superShardKeyList != null && !superShardKeyList.isEmpty()) {
				OracleShardingKeyBuilder superShardKeyBuilder = new OracleDataSource().createShardingKeyBuilder();
				for (ShardKey sk : superShardKeyList) {
					if (sk.getShardKeyType() != null) {
						ShardKeyType skType = ShardKeyType.valueOf(sk.getShardKeyType().toUpperCase());

						if (sk.getShardKeyType().toUpperCase().equals(ShardKeyType.DATE.toString())
								|| sk.getShardKeyType().toUpperCase().equals(ShardKeyType.TIMESTAMP.toString())
								|| sk.getShardKeyType().toUpperCase()
										.equals(ShardKeyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE.toString())) {
							Timestamp ts = Timestamp.valueOf(sk.getShardKeyValue());
							superShardKeyBuilder.subkey(ts, skType.getType());
						} else if (sk.getShardKeyType().toUpperCase().equals(ShardKeyType.RAW.toString())) {
							byte[] bytes = sk.getShardKeyValue().getBytes();
							superShardKeyBuilder.subkey(bytes, skType.getType());
						} else {
							Constructor<?> ctor = skType.getClazz().getConstructor(String.class);
							superShardKeyBuilder.subkey(ctor.newInstance(sk.getShardKeyValue()), skType.getType());
						}
					}
				}
				superShardingKey = superShardKeyBuilder.build();
			}
		}

		catch (SQLException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		Set<ShardInfo> shardInfo = routingCache.getShardInfoForKey(shardingKey, superShardingKey);
		if (shardInfo.isEmpty()) {
			return null;
		}
		
		List<String> shardNames = new ArrayList<String>();
		for(ShardInfo s : shardInfo) {
			shardNames.add(s.getName());
		}
		
		return shardNames;		
	}
	
}
