package com.personblog.article.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArticleStatsMqConfig {

    public static final String ARTICLE_STATS_QUEUE = "article_stats_queue";

    public static final String ARTICLE_STATS_EXCHANGE = "article_stats_exchange";

    public static final String ARTICLE_STATS_KEY = "article.stats.key";

    public static final String ARTICLE_STATS_DLX = "article_stats.dlx";

    public static final String ARTICLE_STATS_DLQ = "article_stats_dlq";

    public static final String ARTICLE_STATS_DLK = "article_stats.dlk";

    @Bean
    public Queue articleStatsQueue() {
        return QueueBuilder.durable(ARTICLE_STATS_QUEUE)
                .deadLetterExchange(ARTICLE_STATS_DLX)
                .deadLetterRoutingKey(ARTICLE_STATS_DLK)
                .build();
    }

    @Bean
    public Queue articleStatsDlq() {
        return QueueBuilder.durable(ARTICLE_STATS_DLQ).build();
    }

    @Bean
    public DirectExchange articleStatsExchange() {
        return new DirectExchange(ARTICLE_STATS_EXCHANGE);
    }

    @Bean
    public FanoutExchange articleStatsDlx() {
        return new FanoutExchange(ARTICLE_STATS_DLX);
    }

    @Bean
    public Binding articleStatsBinding() {
        return BindingBuilder.bind(articleStatsQueue())
                .to(articleStatsExchange())
                .with(ARTICLE_STATS_KEY);
    }

    @Bean
    public Binding articleStatsDlbinding() {
        return BindingBuilder.bind(articleStatsDlq())
                .to(articleStatsDlx());
    }
}
