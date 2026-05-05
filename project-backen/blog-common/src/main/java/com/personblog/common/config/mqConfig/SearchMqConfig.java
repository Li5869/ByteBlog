package com.personblog.common.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索同步 MQ 配置
 * 用于异步同步搜索索引，解耦业务模块和搜索模块
 *
 * @author LSH
 */
@Configuration
public class SearchMqConfig {

    // ========== 队列常量 ==========
    /** 搜索同步队列 */
    public static final String SEARCH_SYNC_QUEUE = "search_sync_queue";

    // ========== 交换机常量 ==========
    /** 搜索交换机 */
    public static final String SEARCH_EXCHANGE = "search_exchange";

    // ========== 路由键常量 ==========
    /** 搜索同步路由键 */
    public static final String SEARCH_SYNC_KEY = "search.sync.key";

    // ========== 死信队列配置 ==========
    /** 死信交换机 */
    public static final String SEARCH_DLX = "search.dlx";
    /** 搜索同步死信队列 */
    public static final String SEARCH_SYNC_DLQ = "search_sync_dlq";
    /** 搜索同步死信路由键 */
    public static final String SEARCH_SYNC_DLK = "search.sync.dlk";

    // ========== 操作类型常量 ==========
    /** 同步操作 */
    public static final String OPERATION_SYNC = "sync";
    /** 删除操作 */
    public static final String OPERATION_DELETE = "delete";

    // ========== 队列 Bean ==========
    @Bean
    public Queue searchSyncQueue() {
        return QueueBuilder.durable(SEARCH_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", SEARCH_DLX)
                .withArgument("x-dead-letter-routing-key", SEARCH_SYNC_DLK)
                .build();
    }

    // ========== 交换机 Bean ==========
    @Bean
    public DirectExchange searchExchange() {
        return new DirectExchange(SEARCH_EXCHANGE, true, false);
    }

    // ========== 绑定 Bean ==========
    @Bean
    public Binding searchSyncBinding() {
        return BindingBuilder.bind(searchSyncQueue()).to(searchExchange()).with(SEARCH_SYNC_KEY);
    }

    // ========== 死信队列 Bean ==========
    @Bean
    public DirectExchange searchDlx() {
        return new DirectExchange(SEARCH_DLX, true, false);
    }

    @Bean
    public Queue searchSyncDlq() {
        return QueueBuilder.durable(SEARCH_SYNC_DLQ).build();
    }

    @Bean
    public Binding searchSyncDlqBinding() {
        return BindingBuilder.bind(searchSyncDlq()).to(searchDlx()).with(SEARCH_SYNC_DLK);
    }
}
