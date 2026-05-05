package com.personblog.common.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InteractionMqConfig {
    //点赞队列
    public static final String LIKE_QUEUE ="like_queue";
    //点赞存库队列
    public static final String LIKE_DB_QUEUE ="like_db_queue";
    //关注队列（用于更新粉丝数和关注数）
    public static final String FOLLOW_QUEUE = "follow_queue";
    //关注通知队列（用于发送关注通知）
    public static final String FOLLOW_NOTIFICATION_QUEUE = "follow_notification_queue";
    //用户点赞队列
    public static final String USER_LIKE_QUEUE = "user_like_queue";
    //收藏队列
    public static final String COLLECTION_QUEUE = "collection_queue";
    //浏览历史队列
    public static final String BROWSE_HISTORY_QUEUE = "browse_history_queue";

    //互动相关交换机
    public static final String INTERACTION_EXCHANGE ="interaction_exchange";


    //点赞key
    public static final String LIKE_KEY = "like.key";
    //存库key
    public static final String LIKE_DB_KEY="like.db.key";

    //关注key（用于更新粉丝数和关注数）
    public static final String FOLLOW_KEY = "follow_key";
    //关注通知key（用于发送关注通知）
    public static final String FOLLOW_NOTIFICATION_KEY = "follow.notification.key";
    //用户点赞key
    public static final String USER_LIKE_KEY = "user.like.key";
    //收藏key
    public static final String COLLECTION_KEY = "collection.key";
    //浏览历史key
    public static final String BROWSE_HISTORY_KEY = "browse.history.key";

    // ========== 死信队列配置 ==========
    // 死信交换机
    public static final String INTERACTION_DLX = "interaction.dlx";
    // like_queue 死信队列
    public static final String LIKE_DLQ = "like_dlq";
    public static final String LIKE_DLK = "like.dlk";
    // like_db_queue 死信队列
    public static final String LIKE_DB_DLQ = "like_db_dlq";
    public static final String LIKE_DB_DLK = "like.db.dlk";
    // follow_queue 死信队列
    public static final String FOLLOW_DLQ = "follow_dlq";
    public static final String FOLLOW_DLK = "follow.dlk";
    // follow_notification_queue 死信队列
    public static final String FOLLOW_NOTIFICATION_DLQ = "follow_notification_dlq";
    public static final String FOLLOW_NOTIFICATION_DLK = "follow.notification.dlk";
    // user_like_queue 死信队列
    public static final String USER_LIKE_DLQ = "user_like_dlq";
    public static final String USER_LIKE_DLK = "user.like.dlk";
    // collection_queue 死信队列
    public static final String COLLECTION_DLQ = "collection_dlq";
    public static final String COLLECTION_DLK = "collection.dlk";
    // browse_history_queue 死信队列
    public static final String BROWSE_HISTORY_DLQ = "browse_history_dlq";
    public static final String BROWSE_HISTORY_DLK = "browse.history.dlk";

    @Bean
    public Queue likeQueue(){
        return QueueBuilder.durable(LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_DLK)
                .build();
    }
    //点赞存库队列
    @Bean
    public Queue likeDbQueue(){
        return QueueBuilder.durable(LIKE_DB_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_DB_DLK)
                .build();
    }
    //关注队列（用于更新粉丝数和关注数）
    @Bean
    public Queue followQueue(){
        return QueueBuilder.durable(FOLLOW_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", FOLLOW_DLK)
                .build();
    }
    //关注通知队列（用于发送关注通知）
    @Bean
    public Queue followNotificationQueue(){
        return QueueBuilder.durable(FOLLOW_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", FOLLOW_NOTIFICATION_DLK)
                .build();
    }
    //用户点赞队列
    @Bean
    public Queue userLikeQueue(){
        return QueueBuilder.durable(USER_LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", USER_LIKE_DLK)
                .build();
    }
    //收藏队列
    @Bean
    public Queue collectionQueue(){
        return QueueBuilder.durable(COLLECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", COLLECTION_DLK)
                .build();
    }
    //浏览历史队列
    @Bean
    public Queue browseHistoryQueue(){
        return QueueBuilder.durable(BROWSE_HISTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", BROWSE_HISTORY_DLK)
                .build();
    }
    //互动相关交换机
    @Bean
    public DirectExchange interactionExchange(){
        return new DirectExchange(INTERACTION_EXCHANGE,true,false);
    }
    //点赞队列绑定
    @Bean
    public Binding likeBinding(){
        return BindingBuilder.bind(likeQueue()).to(interactionExchange()).with(LIKE_KEY);
    }
    //点赞存库队列绑定
    @Bean
    public Binding saveLikeBinding(){
        return BindingBuilder.bind(likeDbQueue()).to(interactionExchange()).with(LIKE_DB_KEY);
    }
    //关注队列绑定
    @Bean
    public Binding followBinding(){
        return BindingBuilder.bind(followQueue()).to(interactionExchange()).with(FOLLOW_KEY);
    }
    //关注通知队列绑定
    @Bean
    public Binding followNotificationBinding(){
        return BindingBuilder.bind(followNotificationQueue()).to(interactionExchange()).with(FOLLOW_NOTIFICATION_KEY);
    }
    //用户点赞队列绑定
    @Bean
    public Binding userLikeBinding(){
        return BindingBuilder.bind(userLikeQueue()).to(interactionExchange()).with(USER_LIKE_KEY);
    }
    //收藏队列绑定
    @Bean
    public Binding collectionBinding(){
        return BindingBuilder.bind(collectionQueue()).to(interactionExchange()).with(COLLECTION_KEY);
    }
    //浏览历史队列绑定
    @Bean
    public Binding browseHistoryBinding(){
        return BindingBuilder.bind(browseHistoryQueue()).to(interactionExchange()).with(BROWSE_HISTORY_KEY);
    }

    // ========== 死信队列 Bean ==========
    // 死信交换机
    @Bean
    public DirectExchange interactionDlx(){
        return new DirectExchange(INTERACTION_DLX, true, false);
    }

    // like_queue 死信队列
    @Bean
    public Queue likeDlq(){
        return QueueBuilder.durable(LIKE_DLQ).build();
    }

    @Bean
    public Binding likeDlqBinding(){
        return BindingBuilder.bind(likeDlq()).to(interactionDlx()).with(LIKE_DLK);
    }

    // like_db_queue 死信队列
    @Bean
    public Queue likeDbDlq(){
        return QueueBuilder.durable(LIKE_DB_DLQ).build();
    }

    @Bean
    public Binding likeDbDlqBinding(){
        return BindingBuilder.bind(likeDbDlq()).to(interactionDlx()).with(LIKE_DB_DLK);
    }

    // follow_queue 死信队列
    @Bean
    public Queue followDlq(){
        return QueueBuilder.durable(FOLLOW_DLQ).build();
    }

    @Bean
    public Binding followDlqBinding(){
        return BindingBuilder.bind(followDlq()).to(interactionDlx()).with(FOLLOW_DLK);
    }

    // follow_notification_queue 死信队列
    @Bean
    public Queue followNotificationDlq(){
        return QueueBuilder.durable(FOLLOW_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding followNotificationDlqBinding(){
        return BindingBuilder.bind(followNotificationDlq()).to(interactionDlx()).with(FOLLOW_NOTIFICATION_DLK);
    }

    // user_like_queue 死信队列
    @Bean
    public Queue userLikeDlq(){
        return QueueBuilder.durable(USER_LIKE_DLQ).build();
    }

    @Bean
    public Binding userLikeDlqBinding(){
        return BindingBuilder.bind(userLikeDlq()).to(interactionDlx()).with(USER_LIKE_DLK);
    }

    // collection_queue 死信队列
    @Bean
    public Queue collectionDlq(){
        return QueueBuilder.durable(COLLECTION_DLQ).build();
    }

    @Bean
    public Binding collectionDlqBinding(){
        return BindingBuilder.bind(collectionDlq()).to(interactionDlx()).with(COLLECTION_DLK);
    }

    // browse_history_queue 死信队列
    @Bean
    public Queue browseHistoryDlq(){
        return QueueBuilder.durable(BROWSE_HISTORY_DLQ).build();
    }

    @Bean
    public Binding browseHistoryDlqBinding(){
        return BindingBuilder.bind(browseHistoryDlq()).to(interactionDlx()).with(BROWSE_HISTORY_DLK);
    }
}
