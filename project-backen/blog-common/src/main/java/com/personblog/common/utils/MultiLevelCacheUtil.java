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

    /** 空值占位，用于缓存穿透防护 */
    private static final String NULL_VALUE = "NULL_VALUE";

    /** 空值本地缓存过期时间（秒） */
    private static final long NULL_VALUE_LOCAL_TTL = 15;

    /** 空值 Redis 缓存过期时间（秒） */
    private static final long NULL_VALUE_REDIS_TTL = 30;

    /** L1 本地缓存：Caffeine，按条目独立控制 TTL */
    private static final Cache<String, CacheEntry> localCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .recordStats()
                    .build();

    public MultiLevelCacheUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取缓存数据（L1 本地 -> L2 Redis -> DB 三级回源）
     *
     * @param key        缓存键
     * @param dataLoader DB 回源加载函数
     * @param ttl        Redis 过期时间（秒）
     * @param localTtl   本地缓存过期时间（秒）
     * @param clazz      返回数据类型
     */
    public <T> T get(String key,
                     Function<String, T> dataLoader,
                     long ttl, long localTtl, Class<T> clazz) {
        String redisKey = CACHE_PREFIX + ":" + key;

        // L1: 查本地缓存
        CacheEntry entry = localCache.getIfPresent(key);
        if (entry != null && !entry.isExpired()) {
            if (entry.getData() == NULL_VALUE) {
                return null;
            }
            return castData(entry.getData(), clazz, key);
        }
        if (entry != null) {
            localCache.invalidate(key);
        }

        // L1 未命中，通过 Caffeine 原子加载 L2 -> DB（防缓存击穿）
        CacheEntry newEntry = localCache.get(key, k -> loadFromRedisOrDB(k, redisKey, dataLoader, ttl, localTtl));
        if (newEntry == null || newEntry.getData() == NULL_VALUE) {
            return null;
        }
        return castData(newEntry.getData(), clazz, key);
    }

    /** 加载 L2 Redis，未命中则回源 DB（由 Caffeine 的原子 get 保证单线程执行） */
    private <T> CacheEntry loadFromRedisOrDB(String key, String redisKey,
                                              Function<String, T> dataLoader,
                                              long ttl, long localTtl) {
        // 双重检查：防止 Caffeine 回调中本地缓存已被其他线程更新
        CacheEntry existing = localCache.getIfPresent(key);
        if (existing != null && !existing.isExpired()) {
            return existing;
        }

        // L2: 查 Redis
        try {
            Object redisValue = redisTemplate.opsForValue().get(redisKey);
            if (redisValue != null) {
                return new CacheEntry(redisValue, localTtl);
            }
        } catch (Exception e) {
            log.warn("Redis获取失败: {}, 继续查询数据库", redisKey, e);
        }

        // L3: 回源 DB
        T data = dataLoader.apply(key);

        if (data != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, data, ttl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis写入失败: {}", redisKey, e);
            }
            return new CacheEntry(data, localTtl);
        }

        // 缓存空值，防缓存穿透
        try {
            redisTemplate.opsForValue().set(redisKey, NULL_VALUE, NULL_VALUE_REDIS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis写入空值标记失败: {}", redisKey, e);
        }
        return new CacheEntry(NULL_VALUE, NULL_VALUE_LOCAL_TTL);
    }

    /** 安全类型转换，类型不匹配时清除缓存并降级返回 null */
    private <T> T castData(Object data, Class<T> clazz, String key) {
        try {
            return clazz.cast(data);
        } catch (ClassCastException e) {
            log.error("缓存数据类型不匹配, key={}, expected={}, actual={}", key, clazz.getSimpleName(), data.getClass().getSimpleName(), e);
            localCache.invalidate(key);
            try {
                redisTemplate.delete(CACHE_PREFIX + ":" + key);
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    /** 删除缓存（同时清除 L1 本地和 L2 Redis） */
    public void evict(String key) {
        String redisKey = CACHE_PREFIX + ":" + key;

        localCache.invalidate(key);

        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis删除失败: {}", redisKey, e);
        }

        log.debug("缓存删除: {}", redisKey);
    }

    /** 清除所有本地缓存 */
    public void clearLocal() {
        localCache.invalidateAll();
        log.info("本地缓存已清除");
    }

    /** 获取本地缓存命中率统计 */
    public String getStats() {
        return localCache.stats().toString();
    }

    /** 缓存条目包装类，携带过期时间，实现键级 TTL 控制 */
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
