/*
 * Copyright 2003-2006 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.jdon.model.query.cache;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.jdon.controller.cache.CacheKey;
import com.jdon.controller.cache.CacheManager;
import com.jdon.model.cache.BlockCacheKeyFactory;

/**
 * query block manager. batch query will get a collection result that it is a
 * block, this block will be cached, next time, we lookup the result in the
 * block that existed in cache, maybe in the block there are those promary keys
 * collection, so reducing visiting database.
 * 
 * the block is made of the primary keys of all models.
 * 
 * @author <a href="mailto:banqiao@jdon.com">banq</a>
 * 
 */
public class BlockCacheManager {
	private final static Logger logger = Logger.getLogger(BlockCacheManager.class);

	public final static String CACHE_TYPE_BLOCK = "BLOCK";

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final CacheManager cacheManager;

	private final List<CacheKey> cacheKeys;

	private final BlockCacheKeyFactory blockCacheKeyFactory;

	public BlockCacheManager(CacheManager cacheManager) {

		this.cacheKeys = new CopyOnWriteArrayList<CacheKey>();
		this.cacheManager = cacheManager;
		this.blockCacheKeyFactory = new BlockCacheKeyFactory();

	}

	public List getBlockKeysFromCache(QueryConditonDatakey qckey) {
		lock.readLock().lock();
		try {
			CacheKey cacheKey = blockCacheKeyFactory.createCacheKey(qckey.getBlockDataKey(), qckey.getSQlKey());
			return (List) cacheManager.fetchObject(cacheKey);
		} finally {
			lock.readLock().unlock();
		}

	}

	public void saveBlockKeys(QueryConditonDatakey qckey, List keys) {
		lock.writeLock().lock();
		try {
			CacheKey cacheKey = blockCacheKeyFactory.createCacheKey(qckey.getBlockDataKey(), qckey.getSQlKey());
			cacheManager.putObect(cacheKey, keys);
			cacheKeys.add(cacheKey);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Integer getAllCountsFromCache(QueryConditonDatakey qckey) {
		lock.readLock().lock();
		try {
			CacheKey cacheKey = blockCacheKeyFactory.createCacheKey(qckey.getBlockDataKey(), qckey.getSQlKey());
			return (Integer) cacheManager.fetchObject(cacheKey);
		} finally {
			lock.readLock().unlock();
		}
	}

	public void saveAllCounts(QueryConditonDatakey qckey, Integer allCount) {
		lock.writeLock().lock();
		try {
			CacheKey cacheKey = blockCacheKeyFactory.createCacheKey(qckey.getBlockDataKey(), qckey.getSQlKey());
			cacheManager.putObect(cacheKey, allCount);
			cacheKeys.add(cacheKey);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void clearCache() {
		if (cacheKeys == null || cacheManager == null)
			return;
		lock.writeLock().lock();
		try {
			Object[] keys = cacheKeys.toArray();
			cacheKeys.clear(); // clear cache as possible quickly

			for (int i = 0; i < keys.length; i++) {// clear the values for all
				// the
				// cachekeys.
				if (keys[i] != null) {
					try {
						CacheKey cacheKey = (CacheKey) keys[i];
						cacheManager.removeObect(cacheKey);
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}

	}
}
