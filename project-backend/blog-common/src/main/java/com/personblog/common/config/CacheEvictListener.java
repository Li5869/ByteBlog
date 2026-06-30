package com.personblog.common.config;

import com.personblog.common.constant.RedisKeys;
import com.personblog.common.utils.MultiLevelCacheUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;

/**
 * Redis Pub/Sub 监听器：接收缓存删除通知，清除本地 Caffeine 缓存
 * 使用 Redisson RTopic 实现，比 Spring RedisMessageListenerContainer 更简洁
 *
 * @author LSH
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheEvictListener {

    private final RedissonClient redissonClient;
    private final MultiLevelCacheUtil multiLevelCacheUtil;

    /**
     * 订阅缓存删除频道，收到消息后清除本地缓存
     * Redisson RTopic 自动处理序列化，消息体直接以 String 形式传递
     */
    @PostConstruct
    public void subscribeEvictChannel() {
        RTopic topic = redissonClient.getTopic(RedisKeys.CACHE_EVICT_CHANNEL);
        topic.addListener(String.class, (channel, key) -> multiLevelCacheUtil.evictLocal(key));
        log.info("已订阅缓存删除频道: {}", RedisKeys.CACHE_EVICT_CHANNEL);
    }
}