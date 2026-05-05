package com.personblog.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RedissonConfig {

    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    private String redisPort;

    @Value("${REDIS_PASSWORD:}")
    private String redisPassword;

    @Value("${REDIS_DATABASE:0}")
    private String redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // Redisson 地址格式：redis://host:port
        String address = "redis://" + redisHost + ":" + redisPort;
        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setDatabase(Integer.parseInt(redisDatabase));
        return Redisson.create(config);
    }
}
