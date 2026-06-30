package com.personblog.comment.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommentMqConfig {

    public static final String AI_COMMENT_QUEUE = "ai_comment_queue";

    public static final String COMMENT_NOTIFICATION_QUEUE = "comment_notification_queue";

    public static final String COMMENT_EXCHANGE = "comment_exchange";
    public static final String COMMENT_ROUTING_KEY = "comment_routing_key";
    public static final String COMMENT_NOTIFICATION_KEY = "comment.notification.key";

    public static final String COMMENT_DLX = "comment.dlx";
    public static final String AI_COMMENT_DLQ = "ai_comment_dlq";
    public static final String AI_COMMENT_DLK = "ai_comment.dlk";
    public static final String COMMENT_NOTIFICATION_DLQ = "comment_notification_dlq";
    public static final String COMMENT_NOTIFICATION_DLK = "comment.notification.dlk";

    @Bean
    public Queue commentQueue() {
        return QueueBuilder.durable(AI_COMMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", COMMENT_DLX)
                .withArgument("x-dead-letter-routing-key", AI_COMMENT_DLK)
                .build();
    }

    @Bean
    public Queue commentNotificationQueue() {
        return QueueBuilder.durable(COMMENT_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", COMMENT_DLX)
                .withArgument("x-dead-letter-routing-key", COMMENT_NOTIFICATION_DLK)
                .build();
    }

    @Bean
    public DirectExchange commentExchange() {
        return new DirectExchange(COMMENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding commentBinding() {
        return BindingBuilder.bind(commentQueue()).to(commentExchange()).with(COMMENT_ROUTING_KEY);
    }

    @Bean
    public Binding commentNotificationBinding() {
        return BindingBuilder.bind(commentNotificationQueue()).to(commentExchange()).with(COMMENT_NOTIFICATION_KEY);
    }

    @Bean
    public DirectExchange commentDlx() {
        return new DirectExchange(COMMENT_DLX, true, false);
    }

    @Bean
    public Queue aiCommentDlq() {
        return QueueBuilder.durable(AI_COMMENT_DLQ).build();
    }

    @Bean
    public Binding aiCommentDlqBinding() {
        return BindingBuilder.bind(aiCommentDlq()).to(commentDlx()).with(AI_COMMENT_DLK);
    }

    @Bean
    public Queue commentNotificationDlq() {
        return QueueBuilder.durable(COMMENT_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding commentNotificationDlqBinding() {
        return BindingBuilder.bind(commentNotificationDlq()).to(commentDlx()).with(COMMENT_NOTIFICATION_DLK);
    }
}
