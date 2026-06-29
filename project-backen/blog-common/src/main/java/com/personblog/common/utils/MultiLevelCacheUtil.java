package com.personblog.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.common.constant.RedisKeys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多级缓存工具类（逻辑过期方案）
 * L1: Caffeine 本地缓存
 * L2: Redis 分布式缓存
 * L3: 数据库回源
 * 逻辑过期方案：
 * - 发现过期立即返回旧数据，用户体验流畅（主线程无阻塞）
 * - 虚拟线程异步重建，分布式锁保护 DB（防止多节点重复回源）
 * - Redis TTL 比逻辑过期时间长，保证重建期间数据不丢失
 * 跨节点一致性：Redis Pub/Sub 广播本地缓存删除通知
 *
 * @author LSH
 */
@Component
@Slf4j
public class MultiLevelCacheUtil {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final Executor cacheRebuildExecutor;

    /** 空值占位，用于缓存穿透防护 */
    private static final String NULL_VALUE = "NULL_VALUE";

    /** 本地缓存最大容量 */
    @Value("${cache.local.max-size:2000}")
    private long localCacheMaxSize;

    /** 空值本地缓存过期时间（秒） */
    @Value("${cache.local.null-ttl:15}")
    private long nullValueLocalTtl;

    /** 空值 Redis 缓存过期时间（秒） */
    @Value("${cache.redis.null-ttl:30}")
    private long nullValueRedisTtl;

    /** 逻辑过期 Redis TTL 缓冲时间（秒），防止重建期间数据被删除 */
    @Value("${cache.logical.buffer-ttl:60}")
    private long logicalBufferTtl;

    /** 分布式锁持有时间（秒） */
    @Value("${cache.lock.lease-time:5}")
    private long lockLeaseTime;

    /** L1 本地缓存：Caffeine，按条目独立控制 TTL */
    private Cache<String, CacheEntry> localCache;

    public MultiLevelCacheUtil(
            RedisTemplate<String, Object> redisTemplate,
            RedissonClient redissonClient,
            @Qualifier("CacheRebuildExecutor") Executor cacheRebuildExecutor) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.cacheRebuildExecutor = cacheRebuildExecutor;
    }

    /** 初始化本地缓存 */
    @PostConstruct
    public void init() {
        localCache = Caffeine.newBuilder()
                .maximumSize(localCacheMaxSize)
                .recordStats()
                .build();
        log.info("多级缓存初始化完成，本地缓存最大容量: {}", localCacheMaxSize);
    }

    /**
     * 获取缓存数据（逻辑过期方案）
     * 发现过期立即返回旧数据，异步重建缓存
     *
     * @param key            缓存键（完整的 Redis Key）
     * @param dataLoader     DB 回源加载函数
     * @param logicalExpire  逻辑过期时间（秒）
     * @param clazz          返回数据类型
     */
    public <T> T get(String key,
                     Function<String, T> dataLoader,
                     long logicalExpire,
                     Class<T> clazz) {
        // L1: 查本地缓存
        CacheEntry entry = localCache.getIfPresent(key);

        if (entry != null) {
            // 逻辑过期判断（数据还在，但已过期）
            if (entry.isExpired()) {
                // 过期了，检查是否已有重建任务
                if (!entry.isRebuilding()) {
                    // 没人在重建，发起异步重建
                    entry.setRebuilding(true);
                    asyncRebuild(key, dataLoader, logicalExpire);
                }
                // 立即返回旧数据（不管有没有重建任务）
                if (entry.getData() == NULL_VALUE) {
                    return null;
                }
                return castData(entry.getData(), clazz, key);
            }
            // 未过期，正常返回
            if (entry.getData() == NULL_VALUE) {
                return null;
            }
            return castData(entry.getData(), clazz, key);
        }

        // 本地缓存完全没数据，走同步加载（首次请求或缓存失效）
        return loadAndReturn(key, dataLoader, logicalExpire, clazz);
    }

    /**
     * 首次加载或缓存失效后的同步加载
     */
    private <T> T loadAndReturn(String key,
                                 Function<String, T> dataLoader,
                                 long logicalExpire,
                                 Class<T> clazz) {
        // L2: 查 Redis
        try {
            Object redisValue = redisTemplate.opsForValue().get(key);
            if (redisValue != null && redisValue != NULL_VALUE) {
                // Redis 有数据，加载到本地缓存
                localCache.put(key, new CacheEntry(redisValue, logicalExpire, false));
                return castData(redisValue, clazz, key);
            }
        } catch (Exception e) {
            log.warn("Redis获取失败: {}, 继续查询数据库", key, e);
        }

        // Redis 无数据，同步回源 DB（首次请求必须等待）
        T data = dataLoader.apply(key);

        if (data != null) {
            // Redis TTL = logicalExpire + buffer（保证重建期间数据不丢失）
            long redisTtl = logicalExpire + logicalBufferTtl;
            try {
                redisTemplate.opsForValue().set(key, data, redisTtl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis写入失败: {}", key, e);
            }
            localCache.put(key, new CacheEntry(data, logicalExpire, false));
            return data;
        }

        // 数据不存在，缓存空值防穿透
        try {
            redisTemplate.opsForValue().set(key, NULL_VALUE, nullValueRedisTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis写入空值标记失败: {}", key, e);
        }
        localCache.put(key, new CacheEntry(NULL_VALUE, nullValueLocalTtl, false));
        return null;
    }

    /**
     * 异步重建缓存（虚拟线程 + 分布式锁）
     * 分布式锁防止多节点同时回源 DB，保护数据库
     */
    private <T> void asyncRebuild(String key,
                                   Function<String, T> dataLoader,
                                   long logicalExpire) {
        cacheRebuildExecutor.execute(() -> {
            String lockKey = RedisKeys.CACHE_LOCK_PREFIX + key;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // tryLock(0, leaseTime) - 不等待，立即返回是否获取成功
                // 只有获取锁的节点才会回源 DB，其他节点直接退出
                if (lock.tryLock(0, lockLeaseTime, TimeUnit.SECONDS)) {
                    try {
                        // 再次检查：防止其他节点已重建完成
                        Object existing = redisTemplate.opsForValue().get(key);
                        if (existing != null && existing != NULL_VALUE) {
                            // 已被其他节点重建，直接更新本地缓存
                            localCache.put(key, new CacheEntry(existing, logicalExpire, false));
                            return;
                        }

                        // 回源 DB（只有持有锁的节点执行）
                        T newData = dataLoader.apply(key);

                        if (newData != null) {
                            // Redis TTL = logicalExpire + buffer
                            long redisTtl = logicalExpire + logicalBufferTtl;
                            redisTemplate.opsForValue().set(key, newData, redisTtl, TimeUnit.SECONDS);
                            // 更新本地缓存
                            localCache.put(key, new CacheEntry(newData, logicalExpire, false));
                            log.debug("缓存异步重建完成: {}", key);
                        } else {
                            // 数据不存在，缓存空值
                            redisTemplate.opsForValue().set(key, NULL_VALUE, nullValueRedisTtl, TimeUnit.SECONDS);
                            localCache.put(key, new CacheEntry(NULL_VALUE, nullValueLocalTtl, false));
                        }
                    } finally {
                        // 释放锁
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    // 获取锁失败，说明其他节点正在重建，直接退出
                    log.debug("其他节点正在重建缓存: {}", key);
                }
            } catch (Exception e) {
                log.error("异步重建缓存失败: {}", key, e);
                // 重建失败，清除 rebuilding 标记，允许下次请求重新发起重建
                CacheEntry entry = localCache.getIfPresent(key);
                if (entry != null) {
                    entry.setRebuilding(false);
                }
            }
        });
    }

    /** 安全类型转换 */
    private <T> T castData(Object data, Class<T> clazz, String key) {
        try {
            return BeanUtil.toBean(data, clazz);
        } catch (ClassCastException e) {
            log.error("缓存数据类型不匹配, key={}, expected={}, actual={}", key, clazz.getSimpleName(), data.getClass().getSimpleName(), e);
            localCache.invalidate(key);
            try {
                redisTemplate.delete(key);
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    /**
     * 删除缓存（同时清除 L1 本地和 L2 Redis）
     * 并通过 Redis Pub/Sub 通知其他节点清除本地缓存
     *
     * @param key 缓存键
     */
    public void evict(String key) {
        // 删除当前节点本地缓存
        localCache.invalidate(key);

        // 删除 Redis 缓存
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis删除失败: {}", key, e);
        }

        // 广播通知其他节点清除本地缓存
        try {
            redisTemplate.convertAndSend(RedisKeys.CACHE_EVICT_CHANNEL, key);
        } catch (Exception e) {
            log.warn("缓存删除广播失败: {}", key, e);
        }

        log.debug("缓存删除: {}", key);
    }

    /**
     * 仅清除本地缓存（供 Redis Pub/Sub 监听器调用）
     *
     * @param key 缓存键
     */
    public void evictLocal(String key) {
        localCache.invalidate(key);
        log.debug("收到广播通知，本地缓存已清除: {}", key);
    }

    /** 缓存条目包装类，携带过期时间和重建标记 */
    private static class CacheEntry {
        private final Object data;
        private final long expireAt;
        private volatile boolean rebuilding;  // 是否正在重建

        CacheEntry(Object data, long logicalExpireSeconds, boolean rebuilding) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + logicalExpireSeconds * 1000;
            this.rebuilding = rebuilding;
        }

        Object getData() {
            return data;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        boolean isRebuilding() {
            return rebuilding;
        }

        void setRebuilding(boolean rebuilding) {
            this.rebuilding = rebuilding;
        }
    }
}