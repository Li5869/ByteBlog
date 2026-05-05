package com.personblog.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(new ObjectMapper());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
    
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(new ObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .computePrefixWith(CacheKeyPrefix.prefixed("cache:"))
                .entryTtl(Duration.ofHours(1));
        // 分场景配置 TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        //文章详情
        cacheConfigurations.put("articleDetail",defaultConfig
                .entryTtl(Duration.ofMinutes(5)));
        // 文章分页 - 5分钟
        cacheConfigurations.put("articlePage", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // 问题详情 - 10分钟
        cacheConfigurations.put("questionDetail", defaultConfig
                .entryTtl(Duration.ofMinutes(10)));

        // 问题分页 - 5分钟
        cacheConfigurations.put("questionPage", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // 用户信息 - 30分钟
        cacheConfigurations.put("userInfo", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));

        // 评论分页 - 5分钟
        cacheConfigurations.put("commentPage", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // 分类 - 1小时
        cacheConfigurations.put("category", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // 标签 - 30分钟
        cacheConfigurations.put("tag", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }


}
