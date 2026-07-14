# RabbitMQ 消息队列规范

## 总体设计

项目采用 RabbitMQ 实现异步事件驱动架构。主请求路径快速响应（Redis + MQ），副作用通过 MQ 最终一致。

### MQ 使用场景

| 场景 | Exchange | Queue | 生产方 | 消费方 |
|------|----------|-------|--------|--------|
| 文章统计更新 | `article_stats_exchange` | `article_stats_queue` | blog-article | blog-article (ArticleStatsMqHandler) |
| 搜索索引同步 | `search_exchange` | `search_sync_queue` | blog-article/blog-question | blog-search (SearchSyncMqHandler) |
| AI 内容审核 | `ai_exchange` | `ai_moderate_queue` | blog-article | blog-ai (AiModerateMqHandler) |
| AI 标题生成 | `ai_exchange` | `ai_title_queue` | blog-comment | blog-ai (AiTitleMqHandler) |
| 点赞持久化 | `interaction_exchange` | `like_db_queue` | blog-interaction | blog-interaction (LikeMqHandler) |
| 点赞计数同步 | `interaction_exchange` | `like_queue` | blog-interaction | blog-interaction (LikeMqHandler) |
| 关注计数更新 | `interaction_exchange` | `follow_queue` | blog-interaction | blog-interaction (FollowMqHandler) |
| 关注通知 | `interaction_exchange` | `follow_notification_queue` | blog-interaction | blog-interaction (NotificationMqHandler) |
| 收藏持久化 | `interaction_exchange` | `collection_queue` | blog-interaction | blog-interaction (CollectionMqHandler) |
| 浏览历史同步 | `interaction_exchange` | `browse_history_queue` | blog-interaction | blog-interaction (BrowseHistoryMqHandler) |

---

## MQ 配置规范

### 配置文件位置

所有 MQ 配置集中在 `blog-common/src/main/java/com/personblog/common/config/mqConfig/` 下，按领域拆分：

- `InteractionMqConfig` — 互动相关（点赞、关注、收藏、浏览历史）
- `AiMqConfig` — AI 相关（审核、标题生成）
- `ArticleStatsMqConfig` — 文章统计
- `CommentMqConfig` — 评论相关
- `SearchMqConfig` — 搜索索引同步

### 队列定义模式

所有队列必须配置死信交换机（DLX），用于失败消息的重试和告警：

```java
@Configuration
public class InteractionMqConfig {

    // 交换机常量
    public static final String INTERACTION_EXCHANGE = "interaction_exchange";
    public static final String INTERACTION_DLX = "interaction_dlx";

    // 队列常量
    public static final String LIKE_QUEUE = "like_queue";
    public static final String LIKE_DLK = "like_dlk";

    // Routing Key 常量
    public static final String LIKE_KEY = "like.key";

    @Bean
    public DirectExchange interactionExchange() {
        return new DirectExchange(INTERACTION_EXCHANGE);
    }

    @Bean
    public DirectExchange interactionDlx() {
        return new DirectExchange(INTERACTION_DLX);
    }

    @Bean
    public Queue likeQueue() {
        return QueueBuilder.durable(LIKE_QUEUE)
                .withArgument("x-dead-letter-exchange", INTERACTION_DLX)
                .withArgument("x-dead-letter-routing-key", LIKE_DLK)
                .build();
    }

    @Bean
    public Queue likeDlq() {
        return QueueBuilder.durable("like_dlq").build();
    }

    @Bean
    public Binding likeBinding() {
        return BindingBuilder.bind(likeQueue())
                .to(interactionExchange())
                .with(LIKE_KEY);
    }

    @Bean
    public Binding likeDlqBinding() {
        return BindingBuilder.bind(likeDlq())
                .to(interactionDlx())
                .with(LIKE_DLK);
    }
}
```

---

## MQ Handler 规范

### Handler 模板

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class LikeMqHandler {

    @RabbitListener(queues = InteractionMqConfig.LIKE_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handleLikeMessage(
            LikeMessageDTO dto,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 1. 业务逻辑
            processLike(dto);

            // 2. 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("点赞消息处理成功: targetType={}, targetId={}",
                     dto.getTargetType(), dto.getTargetId());
        } catch (Exception e) {
            log.error("点赞消息处理失败: {}", e.getMessage(), e);
            // 3. 失败拒绝，进入死信队列（不重新入队）
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("NACK 失败: {}", ex.getMessage());
            }
        }
    }
}
```

### 关键规则

1. **手动 ACK**：必须使用 `channel.basicAck(deliveryTag, false)` 确认
2. **失败 NACK 到死信**：`channel.basicNack(deliveryTag, false, false)` — 不重新入队
3. **try-catch 包裹**：业务逻辑在 try 中，ACK 在 try 中，NACK 在 catch 中
4. **日志记录**：成功用 `log.info`，失败用 `log.error`，包含关键业务参数
5. **containerFactory**：统一使用 `"rabbitListenerContainerFactory"`（全局配置的手动 ACK 工厂）

---

## MQ 消息发送规范

### 发送模板

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SomeServiceImpl {

    private final RabbitTemplate rabbitTemplate;

    private void sendMqMessage(SomeDTO dto) {
        rabbitTemplate.convertAndSend(
            InteractionMqConfig.INTERACTION_EXCHANGE,
            InteractionMqConfig.LIKE_KEY,
            LikeMessageDTO.builder()
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .userId(UserContextHolder.getUserId())
                .isLike(true)
                .build()
        );
        log.info("发送点赞MQ消息: targetId={}", dto.getTargetId());
    }
}
```

### 关键规则

1. **使用常量**：Exchange、Queue、Routing Key 全部引用 MQ 配置类的 `public static final` 常量
2. **Builder 模式**：消息 DTO 使用 Lombok `@Builder` 构建
3. **日志记录**：发送后记录 `log.info`，包含关键业务参数
4. **不处理发送异常**：`RabbitTemplate` 发送失败会抛出异常，由上层事务或调用方处理

---

## 死信队列监控

死信队列（DLQ）用于捕获处理失败的消息。项目中每个业务域都有独立的 DLQ：

- `like_dlq` — 点赞失败消息
- `follow_dlq` — 关注失败消息
- `collection_dlq` — 收藏失败消息
- `article_stats_dlq` — 文章统计失败消息
- `search_sync_dlq` — 搜索同步失败消息
- `ai_moderate_dlq` — AI 审核失败消息

死信消息需要人工排查或通过定时任务重试（参考 `DlqRetryHandler` 在 blog-common 中）。
