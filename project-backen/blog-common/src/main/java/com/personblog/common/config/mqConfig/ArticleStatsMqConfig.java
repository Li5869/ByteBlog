package com.personblog.common.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文章统计更新 MQ 配置
 * <p>
 * 将文章创建/删除后的统计更新操作（用户文章数、标签使用次数、分类文章数、标签缓存清理）
 * 从 CompletableFuture 改为异步 MQ 处理，提升可靠性和可维护性。
 *
 * @author LSH
 */
@Configuration
public class ArticleStatsMqConfig {

    // ==================== 队列 ====================

    /** 文章统计更新队列 */
    public static final String ARTICLE_STATS_QUEUE = "article_stats_queue";

    /** 文章统计更新交换机 */
    public static final String ARTICLE_STATS_EXCHANGE = "article_stats_exchange";

    /** 文章统计更新路由键 */
    public static final String ARTICLE_STATS_KEY = "article.stats.key";

    // ==================== 死信队列 ====================

    /** 文章统计更新死信交换机 */
    public static final String ARTICLE_STATS_DLX = "article_stats.dlx";

    /** 文章统计更新死信队列 */
    public static final String ARTICLE_STATS_DLQ = "article_stats_dlq";

    /** 文章统计更新死信路由键 */
    public static final String ARTICLE_STATS_DLK = "article_stats.dlk";

    // ==================== 队列定义 ====================

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

    // ==================== 交换机定义 ====================

    @Bean
    public DirectExchange articleStatsExchange() {
        return new DirectExchange(ARTICLE_STATS_EXCHANGE);
    }

    @Bean
    public FanoutExchange articleStatsDlx() {
        return new FanoutExchange(ARTICLE_STATS_DLX);
    }

    // ==================== 绑定关系 ====================

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