package com.personblog.search.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchMqConfig {
    public static final String SEARCH_EXCHANGE = "search_exchange";
    public static final String SEARCH_SYNC_QUEUE = "search_sync_queue";

    public static final String SEARCH_SYNC_KEY = "search.sync.key";

    public static final String SEARCH_DLX = "search.dlx";
    public static final String SEARCH_SYNC_DLQ = "search_sync_dlq";
    public static final String SEARCH_SYNC_DLK = "search.sync.dlk";

    public static final String OPERATION_SYNC = "sync";
    public static final String OPERATION_DELETE = "delete";

    @Bean
    public Queue searchSyncQueue() {
        return QueueBuilder.durable(SEARCH_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", SEARCH_DLX)
                .withArgument("x-dead-letter-routing-key", SEARCH_SYNC_DLK)
                .build();
    }

    @Bean
    public DirectExchange searchExchange() {
        return new DirectExchange(SEARCH_EXCHANGE, true, false);
    }

    @Bean
    public Binding searchSyncBinding() {
        return BindingBuilder.bind(searchSyncQueue()).to(searchExchange()).with(SEARCH_SYNC_KEY);
    }

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
