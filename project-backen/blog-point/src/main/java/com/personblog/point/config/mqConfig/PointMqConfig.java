package com.personblog.point.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 积分系统 MQ 配置
 * 定义积分相关的交换机、队列、绑定及死信队列
 *
 * @author LSH
 * @since 2026-06-01
 */
@Configuration
public class PointMqConfig {

    // ===== 队列名常量 =====
    public static final String POINT_SIGN_QUEUE = "point_sign_queue";
    public static final String POINT_ARTICLE_QUEUE = "point_article_queue";
    public static final String POINT_LIKE_QUEUE = "point_like_queue";
    public static final String POINT_COLLECTION_QUEUE = "point_collection_queue";
    public static final String POINT_ADMIN_ADJUST_QUEUE = "point_admin_adjust_queue";

    // ===== 交换机名 =====
    public static final String POINT_EXCHANGE = "point_exchange";

    // ===== 路由键 =====
    public static final String POINT_SIGN_KEY = "point.sign.key";
    public static final String POINT_ARTICLE_KEY = "point.article.key";
    public static final String POINT_LIKE_KEY = "point.like.key";
    public static final String POINT_COLLECTION_KEY = "point.collection.key";
    public static final String POINT_ADMIN_ADJUST_KEY = "point.admin.adjust.key";

    // ===== 死信交换机 & 死信队列 & 死信路由键 =====
    public static final String POINT_DLX = "point.dlx";
    public static final String POINT_SIGN_DLQ = "point_sign_dlq";
    public static final String POINT_SIGN_DLK = "point.sign.dlk";
    public static final String POINT_ARTICLE_DLQ = "point_article_dlq";
    public static final String POINT_ARTICLE_DLK = "point.article.dlk";
    public static final String POINT_LIKE_DLQ = "point_like_dlq";
    public static final String POINT_LIKE_DLK = "point.like.dlk";
    public static final String POINT_COLLECTION_DLQ = "point_collection_dlq";
    public static final String POINT_COLLECTION_DLK = "point.collection.dlk";
    public static final String POINT_ADMIN_ADJUST_DLQ = "point_admin_adjust_dlq";
    public static final String POINT_ADMIN_ADJUST_DLK = "point.admin.adjust.dlk";

    // ===== 业务队列 Bean（绑定死信） =====

    @Bean
    public Queue pointSignQueue() {
        return QueueBuilder.durable(POINT_SIGN_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_SIGN_DLK)
                .build();
    }

    @Bean
    public Queue pointArticleQueue() {
        return QueueBuilder.durable(POINT_ARTICLE_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_ARTICLE_DLK)
                .build();
    }

    @Bean
    public Queue pointLikeQueue() {
        return QueueBuilder.durable(POINT_LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_LIKE_DLK)
                .build();
    }

    @Bean
    public Queue pointCollectionQueue() {
        return QueueBuilder.durable(POINT_COLLECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_COLLECTION_DLK)
                .build();
    }

    @Bean
    public Queue pointAdminAdjustQueue() {
        return QueueBuilder.durable(POINT_ADMIN_ADJUST_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_ADMIN_ADJUST_DLK)
                .build();
    }

    // ===== 业务交换机 & 绑定 =====

    @Bean
    public DirectExchange pointExchange() {
        return new DirectExchange(POINT_EXCHANGE, true, false);
    }

    @Bean
    public Binding pointSignBinding() {
        return BindingBuilder.bind(pointSignQueue()).to(pointExchange()).with(POINT_SIGN_KEY);
    }

    @Bean
    public Binding pointArticleBinding() {
        return BindingBuilder.bind(pointArticleQueue()).to(pointExchange()).with(POINT_ARTICLE_KEY);
    }

    @Bean
    public Binding pointLikeBinding() {
        return BindingBuilder.bind(pointLikeQueue()).to(pointExchange()).with(POINT_LIKE_KEY);
    }

    @Bean
    public Binding pointCollectionBinding() {
        return BindingBuilder.bind(pointCollectionQueue()).to(pointExchange()).with(POINT_COLLECTION_KEY);
    }

    @Bean
    public Binding pointAdminAdjustBinding() {
        return BindingBuilder.bind(pointAdminAdjustQueue()).to(pointExchange()).with(POINT_ADMIN_ADJUST_KEY);
    }

    // ===== 死信交换机 & 死信队列 Bean & 绑定 =====

    @Bean
    public DirectExchange pointDlx() {
        return new DirectExchange(POINT_DLX, true, false);
    }

    @Bean
    public Queue pointSignDlq() {
        return QueueBuilder.durable(POINT_SIGN_DLQ).build();
    }

    @Bean
    public Queue pointArticleDlq() {
        return QueueBuilder.durable(POINT_ARTICLE_DLQ).build();
    }

    @Bean
    public Queue pointLikeDlq() {
        return QueueBuilder.durable(POINT_LIKE_DLQ).build();
    }

    @Bean
    public Queue pointCollectionDlq() {
        return QueueBuilder.durable(POINT_COLLECTION_DLQ).build();
    }

    @Bean
    public Queue pointAdminAdjustDlq() {
        return QueueBuilder.durable(POINT_ADMIN_ADJUST_DLQ).build();
    }

    @Bean
    public Binding pointSignDlqBinding() {
        return BindingBuilder.bind(pointSignDlq()).to(pointDlx()).with(POINT_SIGN_DLK);
    }

    @Bean
    public Binding pointArticleDlqBinding() {
        return BindingBuilder.bind(pointArticleDlq()).to(pointDlx()).with(POINT_ARTICLE_DLK);
    }

    @Bean
    public Binding pointLikeDlqBinding() {
        return BindingBuilder.bind(pointLikeDlq()).to(pointDlx()).with(POINT_LIKE_DLK);
    }

    @Bean
    public Binding pointCollectionDlqBinding() {
        return BindingBuilder.bind(pointCollectionDlq()).to(pointDlx()).with(POINT_COLLECTION_DLK);
    }

    @Bean
    public Binding pointAdminAdjustDlqBinding() {
        return BindingBuilder.bind(pointAdminAdjustDlq()).to(pointDlx()).with(POINT_ADMIN_ADJUST_DLK);
    }
}
