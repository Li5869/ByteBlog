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

    /** 签到积分发放队列 */
    public static final String POINT_SIGN_QUEUE = "point_sign_queue";
    /** 文章发布积分发放队列 */
    public static final String POINT_ARTICLE_QUEUE = "point_article_queue";
    /** 点赞积分发放队列 */
    public static final String POINT_LIKE_QUEUE = "point_like_queue";
    /** 收藏积分发放队列 */
    public static final String POINT_COLLECTION_QUEUE = "point_collection_queue";
    /** 管理员调整积分队列 */
    public static final String POINT_ADMIN_ADJUST_QUEUE = "point_admin_adjust_queue";

    // ===== 交换机名 =====

    /** 积分业务交换机 */
    public static final String POINT_EXCHANGE = "point_exchange";

    // ===== 路由键 =====

    /** 签到积分路由键 */
    public static final String POINT_SIGN_KEY = "point.sign.key";
    /** 文章积分路由键 */
    public static final String POINT_ARTICLE_KEY = "point.article.key";
    /** 点赞积分路由键 */
    public static final String POINT_LIKE_KEY = "point.like.key";
    /** 收藏积分路由键 */
    public static final String POINT_COLLECTION_KEY = "point.collection.key";
    /** 管理员调整积分路由键 */
    public static final String POINT_ADMIN_ADJUST_KEY = "point.admin.adjust.key";

    // ===== 死信交换机 & 死信队列 & 死信路由键 =====

    /** 积分死信交换机 */
    public static final String POINT_DLX = "point.dlx";
    /** 签到积分死信队列 */
    public static final String POINT_SIGN_DLQ = "point_sign_dlq";
    public static final String POINT_SIGN_DLK = "point.sign.dlk";
    /** 文章积分死信队列 */
    public static final String POINT_ARTICLE_DLQ = "point_article_dlq";
    public static final String POINT_ARTICLE_DLK = "point.article.dlk";
    /** 点赞积分死信队列 */
    public static final String POINT_LIKE_DLQ = "point_like_dlq";
    public static final String POINT_LIKE_DLK = "point.like.dlk";
    /** 收藏积分死信队列 */
    public static final String POINT_COLLECTION_DLQ = "point_collection_dlq";
    public static final String POINT_COLLECTION_DLK = "point.collection.dlk";
    /** 管理员调整积分死信队列 */
    public static final String POINT_ADMIN_ADJUST_DLQ = "point_admin_adjust_dlq";
    public static final String POINT_ADMIN_ADJUST_DLK = "point.admin.adjust.dlk";

    // ===== 业务队列 Bean（绑定死信） =====

    /** 签到积分队列，消费失败转入 point_sign_dlq */
    @Bean
    public Queue pointSignQueue() {
        return QueueBuilder.durable(POINT_SIGN_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_SIGN_DLK)
                .build();
    }

    /** 文章积分队列，消费失败转入 point_article_dlq */
    @Bean
    public Queue pointArticleQueue() {
        return QueueBuilder.durable(POINT_ARTICLE_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_ARTICLE_DLK)
                .build();
    }

    /** 点赞积分队列，消费失败转入 point_like_dlq */
    @Bean
    public Queue pointLikeQueue() {
        return QueueBuilder.durable(POINT_LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_LIKE_DLK)
                .build();
    }

    /** 收藏积分队列，消费失败转入 point_collection_dlq */
    @Bean
    public Queue pointCollectionQueue() {
        return QueueBuilder.durable(POINT_COLLECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_COLLECTION_DLK)
                .build();
    }

    /** 管理员调整积分队列，消费失败转入 point_admin_adjust_dlq */
    @Bean
    public Queue pointAdminAdjustQueue() {
        return QueueBuilder.durable(POINT_ADMIN_ADJUST_QUEUE)
                .withArgument("x-dead-letter-exchange", POINT_DLX)
                .withArgument("x-dead-letter-routing-key", POINT_ADMIN_ADJUST_DLK)
                .build();
    }

    // ===== 业务交换机 & 绑定 =====

    /** 积分业务交换机（DirectExchange，持久化） */
    @Bean
    public DirectExchange pointExchange() {
        return new DirectExchange(POINT_EXCHANGE, true, false);
    }

    /** 签到队列绑定到业务交换机 */
    @Bean
    public Binding pointSignBinding() {
        return BindingBuilder.bind(pointSignQueue()).to(pointExchange()).with(POINT_SIGN_KEY);
    }

    /** 文章队列绑定到业务交换机 */
    @Bean
    public Binding pointArticleBinding() {
        return BindingBuilder.bind(pointArticleQueue()).to(pointExchange()).with(POINT_ARTICLE_KEY);
    }

    /** 点赞队列绑定到业务交换机 */
    @Bean
    public Binding pointLikeBinding() {
        return BindingBuilder.bind(pointLikeQueue()).to(pointExchange()).with(POINT_LIKE_KEY);
    }

    /** 收藏队列绑定到业务交换机 */
    @Bean
    public Binding pointCollectionBinding() {
        return BindingBuilder.bind(pointCollectionQueue()).to(pointExchange()).with(POINT_COLLECTION_KEY);
    }

    /** 管理员调整队列绑定到业务交换机 */
    @Bean
    public Binding pointAdminAdjustBinding() {
        return BindingBuilder.bind(pointAdminAdjustQueue()).to(pointExchange()).with(POINT_ADMIN_ADJUST_KEY);
    }

    // ===== 死信交换机 & 死信队列 Bean & 绑定 =====

    /** 积分死信交换机（DirectExchange，持久化） */
    @Bean
    public DirectExchange pointDlx() {
        return new DirectExchange(POINT_DLX, true, false);
    }

    /** 签到积分死信队列 */
    @Bean
    public Queue pointSignDlq() {
        return QueueBuilder.durable(POINT_SIGN_DLQ).build();
    }

    /** 文章积分死信队列 */
    @Bean
    public Queue pointArticleDlq() {
        return QueueBuilder.durable(POINT_ARTICLE_DLQ).build();
    }

    /** 点赞积分死信队列 */
    @Bean
    public Queue pointLikeDlq() {
        return QueueBuilder.durable(POINT_LIKE_DLQ).build();
    }

    /** 收藏积分死信队列 */
    @Bean
    public Queue pointCollectionDlq() {
        return QueueBuilder.durable(POINT_COLLECTION_DLQ).build();
    }

    /** 管理员调整积分死信队列 */
    @Bean
    public Queue pointAdminAdjustDlq() {
        return QueueBuilder.durable(POINT_ADMIN_ADJUST_DLQ).build();
    }

    /** 签到死信队列绑定到死信交换机 */
    @Bean
    public Binding pointSignDlqBinding() {
        return BindingBuilder.bind(pointSignDlq()).to(pointDlx()).with(POINT_SIGN_DLK);
    }

    /** 文章死信队列绑定到死信交换机 */
    @Bean
    public Binding pointArticleDlqBinding() {
        return BindingBuilder.bind(pointArticleDlq()).to(pointDlx()).with(POINT_ARTICLE_DLK);
    }

    /** 点赞死信队列绑定到死信交换机 */
    @Bean
    public Binding pointLikeDlqBinding() {
        return BindingBuilder.bind(pointLikeDlq()).to(pointDlx()).with(POINT_LIKE_DLK);
    }

    /** 收藏死信队列绑定到死信交换机 */
    @Bean
    public Binding pointCollectionDlqBinding() {
        return BindingBuilder.bind(pointCollectionDlq()).to(pointDlx()).with(POINT_COLLECTION_DLK);
    }

    /** 管理员调整死信队列绑定到死信交换机 */
    @Bean
    public Binding pointAdminAdjustDlqBinding() {
        return BindingBuilder.bind(pointAdminAdjustDlq()).to(pointDlx()).with(POINT_ADMIN_ADJUST_DLK);
    }
}
