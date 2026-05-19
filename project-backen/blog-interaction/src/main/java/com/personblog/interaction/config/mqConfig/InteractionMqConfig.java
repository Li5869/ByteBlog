package com.personblog.interaction.config.mqConfig;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InteractionMqConfig {
    public static final String LIKE_QUEUE ="like_queue";
    public static final String LIKE_DB_QUEUE ="like_db_queue";
    public static final String FOLLOW_QUEUE = "follow_queue";
    public static final String FOLLOW_NOTIFICATION_QUEUE = "follow_notification_queue";
    public static final String USER_LIKE_QUEUE = "user_like_queue";
    public static final String COLLECTION_QUEUE = "collection_queue";
    public static final String BROWSE_HISTORY_QUEUE = "browse_history_queue";
    public static final String LIKE_SYNC_CACHE_QUEUE = "like_sync_cache_queue";

    public static final String INTERACTION_EXCHANGE ="interaction_exchange";

    public static final String LIKE_KEY = "like.key";
    public static final String LIKE_DB_KEY="like.db.key";
    public static final String FOLLOW_KEY = "follow_key";
    public static final String FOLLOW_NOTIFICATION_KEY = "follow.notification.key";
    public static final String USER_LIKE_KEY = "user.like.key";
    public static final String COLLECTION_KEY = "collection.key";
    public static final String BROWSE_HISTORY_KEY = "browse.history.key";
    public static final String LIKE_SYNC_CACHE_KEY = "like.sync.cache.key";

    public static final String INTERACTION_DLX = "interaction.dlx";
    public static final String LIKE_DLQ = "like_dlq";
    public static final String LIKE_DLK = "like.dlk";
    public static final String LIKE_DB_DLQ = "like_db_dlq";
    public static final String LIKE_DB_DLK = "like.db.dlk";
    public static final String FOLLOW_DLQ = "follow_dlq";
    public static final String FOLLOW_DLK = "follow.dlk";
    public static final String FOLLOW_NOTIFICATION_DLQ = "follow_notification_dlq";
    public static final String FOLLOW_NOTIFICATION_DLK = "follow.notification.dlk";
    public static final String USER_LIKE_DLQ = "user_like_dlq";
    public static final String USER_LIKE_DLK = "user.like.dlk";
    public static final String COLLECTION_DLQ = "collection_dlq";
    public static final String COLLECTION_DLK = "collection.dlk";
    public static final String BROWSE_HISTORY_DLQ = "browse_history_dlq";
    public static final String BROWSE_HISTORY_DLK = "browse.history.dlk";
    public static final String LIKE_SYNC_CACHE_DLQ = "like_sync_cache_dlq";
    public static final String LIKE_SYNC_CACHE_DLK = "like.sync.cache.dlk";

    @Bean
    public Queue likeQueue(){
        return QueueBuilder.durable(LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_DLK)
                .build();
    }
    @Bean
    public Queue likeDbQueue(){
        return QueueBuilder.durable(LIKE_DB_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_DB_DLK)
                .build();
    }
    @Bean
    public Queue followQueue(){
        return QueueBuilder.durable(FOLLOW_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", FOLLOW_DLK)
                .build();
    }
    @Bean
    public Queue followNotificationQueue(){
        return QueueBuilder.durable(FOLLOW_NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", FOLLOW_NOTIFICATION_DLK)
                .build();
    }
    @Bean
    public Queue userLikeQueue(){
        return QueueBuilder.durable(USER_LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", USER_LIKE_DLK)
                .build();
    }
    @Bean
    public Queue collectionQueue(){
        return QueueBuilder.durable(COLLECTION_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", COLLECTION_DLK)
                .build();
    }
    @Bean
    public Queue browseHistoryQueue(){
        return QueueBuilder.durable(BROWSE_HISTORY_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", BROWSE_HISTORY_DLK)
                .build();
    }
    @Bean
    public Queue likeSyncCacheQueue(){
        return QueueBuilder.durable(LIKE_SYNC_CACHE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_SYNC_CACHE_DLK)
                .build();
    }
    @Bean
    public DirectExchange interactionExchange(){
        return new DirectExchange(INTERACTION_EXCHANGE,true,false);
    }
    @Bean
    public Binding likeBinding(){
        return BindingBuilder.bind(likeQueue()).to(interactionExchange()).with(LIKE_KEY);
    }
    @Bean
    public Binding saveLikeBinding(){
        return BindingBuilder.bind(likeDbQueue()).to(interactionExchange()).with(LIKE_DB_KEY);
    }
    @Bean
    public Binding followBinding(){
        return BindingBuilder.bind(followQueue()).to(interactionExchange()).with(FOLLOW_KEY);
    }
    @Bean
    public Binding followNotificationBinding(){
        return BindingBuilder.bind(followNotificationQueue()).to(interactionExchange()).with(FOLLOW_NOTIFICATION_KEY);
    }
    @Bean
    public Binding userLikeBinding(){
        return BindingBuilder.bind(userLikeQueue()).to(interactionExchange()).with(USER_LIKE_KEY);
    }
    @Bean
    public Binding collectionBinding(){
        return BindingBuilder.bind(collectionQueue()).to(interactionExchange()).with(COLLECTION_KEY);
    }
    @Bean
    public Binding browseHistoryBinding(){
        return BindingBuilder.bind(browseHistoryQueue()).to(interactionExchange()).with(BROWSE_HISTORY_KEY);
    }
    @Bean
    public Binding likeSyncCacheBinding(){
        return BindingBuilder.bind(likeSyncCacheQueue()).to(interactionExchange()).with(LIKE_SYNC_CACHE_KEY);
    }

    @Bean
    public DirectExchange interactionDlx(){
        return new DirectExchange(INTERACTION_DLX, true, false);
    }

    @Bean
    public Queue likeDlq(){
        return QueueBuilder.durable(LIKE_DLQ).build();
    }

    @Bean
    public Binding likeDlqBinding(){
        return BindingBuilder.bind(likeDlq()).to(interactionDlx()).with(LIKE_DLK);
    }

    @Bean
    public Queue likeDbDlq(){
        return QueueBuilder.durable(LIKE_DB_DLQ).build();
    }

    @Bean
    public Binding likeDbDlqBinding(){
        return BindingBuilder.bind(likeDbDlq()).to(interactionDlx()).with(LIKE_DB_DLK);
    }

    @Bean
    public Queue followDlq(){
        return QueueBuilder.durable(FOLLOW_DLQ).build();
    }

    @Bean
    public Binding followDlqBinding(){
        return BindingBuilder.bind(followDlq()).to(interactionDlx()).with(FOLLOW_DLK);
    }

    @Bean
    public Queue followNotificationDlq(){
        return QueueBuilder.durable(FOLLOW_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Binding followNotificationDlqBinding(){
        return BindingBuilder.bind(followNotificationDlq()).to(interactionDlx()).with(FOLLOW_NOTIFICATION_DLK);
    }

    @Bean
    public Queue userLikeDlq(){
        return QueueBuilder.durable(USER_LIKE_DLQ).build();
    }

    @Bean
    public Binding userLikeDlqBinding(){
        return BindingBuilder.bind(userLikeDlq()).to(interactionDlx()).with(USER_LIKE_DLK);
    }

    @Bean
    public Queue collectionDlq(){
        return QueueBuilder.durable(COLLECTION_DLQ).build();
    }

    @Bean
    public Binding collectionDlqBinding(){
        return BindingBuilder.bind(collectionDlq()).to(interactionDlx()).with(COLLECTION_DLK);
    }

    @Bean
    public Queue browseHistoryDlq(){
        return QueueBuilder.durable(BROWSE_HISTORY_DLQ).build();
    }

    @Bean
    public Binding browseHistoryDlqBinding(){
        return BindingBuilder.bind(browseHistoryDlq()).to(interactionDlx()).with(BROWSE_HISTORY_DLK);
    }

    @Bean
    public Queue likeSyncCacheDlq(){
        return QueueBuilder.durable(LIKE_SYNC_CACHE_DLQ).build();
    }

    @Bean
    public Binding likeSyncCacheDlqBinding(){
        return BindingBuilder.bind(likeSyncCacheDlq()).to(interactionDlx()).with(LIKE_SYNC_CACHE_DLK);
    }
}
