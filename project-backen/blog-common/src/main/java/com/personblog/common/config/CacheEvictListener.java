package com.personblog.common.config;

import com.personblog.common.constant.RedisKeys;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 监听器：接收缓存删除通知，清除本地 Caffeine 缓存
 * 实现多级缓存的跨节点一致性
 *
 * @author LSH
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CacheEvictListener {

    private final MultiLevelCacheUtil multiLevelCacheUtil;

    /**
     * 创建消息监听适配器，直接将 multiLevelCacheUtil.evictLocal 作为委托方法
     * MessageListenerAdapter 会自动将消息体反序列化为 String 并调用 evictLocal(key)
     */
    @Bean
    public MessageListenerAdapter cacheEvictListenerAdapter() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(multiLevelCacheUtil, "evictLocal");
        // 指定 String 序列化器，确保消息体正确反序列化为 String（而非默认 JDK 序列化）
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    /**
     * 注册监听容器，绑定缓存删除频道
     */
    @Bean
    public RedisMessageListenerContainer cacheEvictListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter cacheEvictListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheEvictListenerAdapter,
                new ChannelTopic(RedisKeys.CACHE_EVICT_CHANNEL));
        return container;
    }
}