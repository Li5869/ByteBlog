package com.personblog.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@Slf4j
public class MultiLevelCacheUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    /** 空值占位，用于缓存穿透防护 */
    private static final String NULL_VALUE = "NULL_VALUE";

    /** 本地缓存最大容量（可通过配置文件动态配置） */
    @Value("${cache.local.max-size:2000}")
    private long localCacheMaxSize;

    /** 空值本地缓存过期时间（秒），可通过配置文件动态配置 */
    @Value("${cache.local.null-ttl:15}")
    private long nullValueLocalTtl;

    /** 空值 Redis 缓存过期时间（秒），可通过配置文件动态配置 */
    @Value("${cache.redis.null-ttl:30}")
    private long nullValueRedisTtl;

    /** L1 本地缓存：Caffeine，按条目独立控制 TTL */
    private Cache<String, CacheEntry> localCache;

    public MultiLevelCacheUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 初始化本地缓存（在依赖注入完成后执行） */
    @PostConstruct
    public void init() {
        localCache = Caffeine.newBuilder()
                .maximumSize(localCacheMaxSize)
                .recordStats()
                .build();
        log.info("多级缓存初始化完成，本地缓存最大容量: {}", localCacheMaxSize);
    }

    /**
     * 获取缓存数据（L1 本地 -> L2 Redis -> DB 三级回源）
     *
     * @param key        缓存键（完整的 Redis Key，如 article:metadata:123）
     * @param dataLoader DB 回源加载函数
     * @param ttl        Redis 过期时间（秒）
     * @param localTtl   本地缓存过期时间（秒）
     * @param clazz      返回数据类型
     */
    public <T> T get(String key,
                     Function<String, T> dataLoader,
                     long ttl, long localTtl, Class<T> clazz) {
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
        CacheEntry newEntry = localCache.get(key, k -> loadFromRedisOrDB(k, key, dataLoader, ttl, localTtl));
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
            redisTemplate.opsForValue().set(redisKey, NULL_VALUE, nullValueRedisTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis写入空值标记失败: {}", redisKey, e);
        }
        return new CacheEntry(NULL_VALUE, nullValueLocalTtl);
    }

    /** 安全类型转换，类型不匹配时清除缓存并降级返回 null */
    private <T> T castData(Object data, Class<T> clazz, String key) {
        try {
            return BeanUtil.toBean(data,clazz);
        } catch (ClassCastException e) {
            log.error("缓存数据类型不匹配, key={}, expected={}, actual={}", key, clazz.getSimpleName(), data.getClass().getSimpleName(), e);
            localCache.invalidate(key);
            try {
                // 直接使用 key 删除 Redis 缓存
                redisTemplate.delete(key);
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    /**
     * 删除缓存（同时清除 L1 本地和 L2 Redis）
     *
     * @param key 缓存键（完整的 Redis Key，如 article:metadata:123）
     */
    public void evict(String key) {
        // 直接使用传入的 key 作为 Redis Key
        localCache.invalidate(key);

        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis删除失败: {}", key, e);
        }

        log.debug("缓存删除: {}", key);
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
