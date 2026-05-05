package com.personblog.common.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultiLevelCacheUtil {
    private final RedisTemplate<String,Object> redisTemplate;

    private static final String CACHE_PREFIX = "cache";
    
    private static final Cache<String,Object> localCache=
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .recordStats()
                    .build();
    private long localTtl;

    public <T> T get( String key,
                     Function<String, T> dataLoader,
                     long ttl, long localTtl, Class<T> clazz){
        this.localTtl = localTtl;
        String redisKey = CACHE_PREFIX + ":" + key;
        //先查本地缓存
        Object localValue = localCache.getIfPresent(key);
        if(localValue!=null){
            return clazz.cast(localValue);
        }
        //再查redis缓存
        try {
            Object redisValue = redisTemplate.opsForValue().get(redisKey);
            if(redisValue!=null){
                localCache.put(key,redisValue);
                return clazz.cast(redisValue);
            }
        }
        catch (Exception e){
            log.warn("Redis获取失败: {}, 继续查询数据库", redisKey, e);
        }
        T data = dataLoader.apply(key);

        if(data!=null){
            try {
                redisTemplate.opsForValue().set(redisKey,data,ttl,TimeUnit.SECONDS);
            }catch (Exception e){
                log.warn("Redis写入失败: {}", redisKey, e);
            }
            localCache.put(key,data);
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

}
