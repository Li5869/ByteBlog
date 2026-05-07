package com.personblog.common.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class MultiLevelCacheUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "cache";

    // 本地缓存：TTL 由每个条目自行管理（通过 CacheEntry 包装）
    private static final Cache<String, CacheEntry> localCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .recordStats()
                    .build();

    public MultiLevelCacheUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取缓存数据（多级缓存：L1 本地 -> L2 Redis -> DB）
     *
     * @param key        缓存键
     * @param dataLoader 数据加载函数（DB 回源）
     * @param ttl        Redis 缓存过期时间（秒）
     * @param localTtl   本地缓存过期时间（秒）
     * @param clazz      数据类型
     * @return 数据
     */
    public <T> T get(String key,
                     Function<String, T> dataLoader,
                     long ttl, long localTtl, Class<T> clazz) {
        String redisKey = CACHE_PREFIX + ":" + key;

        // 先查本地缓存（检查条目是否过期）
        CacheEntry entry = localCache.getIfPresent(key);
        if (entry != null && !entry.isExpired()) {
            return clazz.cast(entry.getData());
        }
        // 本地缓存过期或不存在，移除旧条目
        if (entry != null) {
            localCache.invalidate(key);
        }

        // 再查 Redis 缓存
        try {
            Object redisValue = redisTemplate.opsForValue().get(redisKey);
            if (redisValue != null) {
                // 从 Redis 加载后放入本地缓存（使用调用方指定的 localTtl）
                localCache.put(key, new CacheEntry(redisValue, localTtl));
                return clazz.cast(redisValue);
            }
        } catch (Exception e) {
            log.warn("Redis获取失败: {}, 继续查询数据库", redisKey, e);
        }

        // 回源到数据库
        T data = dataLoader.apply(key);

        if (data != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, data, ttl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis写入失败: {}", redisKey, e);
            }
            // 写入本地缓存（使用调用方指定的 localTtl）
            localCache.put(key, new CacheEntry(data, localTtl));
        }
        return data;
    }

    /**
     * 删除缓存（同时删除 L1 和 L2）
     */
    public void evict(String key) {
        String redisKey = CACHE_PREFIX + ":" + key;

        // 删除本地缓存
        localCache.invalidate(key);

        // 删除 Redis 缓存
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis删除失败: {}", redisKey, e);
        }

        log.debug("缓存删除: {}", redisKey);
    }

    /**
     * 清除所有本地缓存
     */
    public void clearLocal() {
        localCache.invalidateAll();
        log.info("本地缓存已清除");
    }

    /**
     * 获取缓存命中率统计
     */
    public String getStats() {
        return localCache.stats().toString();
    }

    /**
     * 缓存条目包装类，携带过期时间，实现每个条目独立的 TTL 控制
     */
    private static class CacheEntry {
        private final Object data;
        private final long expireAt;

        CacheEntry(Object data, long localTtlSeconds) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + localTtlSeconds * 1000;
        }

        Object getData() {
            return data;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
