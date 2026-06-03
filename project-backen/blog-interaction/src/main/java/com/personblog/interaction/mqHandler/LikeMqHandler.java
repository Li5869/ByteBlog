package com.personblog.interaction.mqHandler;

import com.personblog.api.articleAPI.ArticleMqAPI;
import com.personblog.api.interactionAPI.CommentApi;
import com.personblog.api.interactionAPI.NotificationApi;
import com.personblog.api.pointAPI.PointMqApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;
import com.personblog.common.dto.MqMessage.Interaction.LikeSaveDBMessage;
import com.personblog.common.dto.MqMessage.Interaction.SyncLikeCacheMessage;
import com.personblog.common.dto.MqMessage.notifaction.NotificationMessage;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.interaction.bizService.BizLikeService;
import com.personblog.interaction.service.ArticleLikeService;
import com.personblog.interaction.service.CommentLikeService;
import com.personblog.interaction.strategy.LikeStrategy;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;
import static com.personblog.interaction.config.mqConfig.InteractionMqConfig.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeMqHandler {
    private final ArticleMqAPI articleAPI;
    private final CommentApi commentApi;
    private final BizLikeService likeService;
    private final ArticleLikeService articleLikeService;
    private final CommentLikeService commentLikeService;
    private final RedissonClient redissonClient;
    private final NotificationApi notificationApi;
    private final UseApi useApi;
    private final PointMqApi pointMqApi;

    private final Map<String, LikeStrategy> likeStrategyMap = new HashMap<>();

    private final Map<String, Consumer<List<LikeMessage>>> likeCountUpdaters = new HashMap<>(4);

    // 点赞缓存同步锁前缀
    private static final String LIKE_SYNC_LOCK_PREFIX = "like_sync_cache:";

    @PostConstruct
    public void init() {
        likeStrategyMap.put(ARTICLE, articleLikeService);
        likeStrategyMap.put(COMMENT, commentLikeService);

        likeCountUpdaters.put(ARTICLE, articleAPI::updateLikeCount);
        likeCountUpdaters.put(COMMENT, commentApi::updateLikeCount);
    }
    @RabbitListener(queues = LIKE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handlerLikeMessage(List<LikeMessage> dtos, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            Map<String, List<LikeMessage>> map = dtos.stream()
                    .collect(Collectors.groupingBy(com.personblog.common.dto.MqMessage.Interaction.LikeMessage::getTargetType));

            likeCountUpdaters.forEach((type, updater) -> {
                List<LikeMessage> list = map.get(type);
                if (list != null) {
                    updater.accept(list);
                }
            });

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("点赞消息处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = LIKE_DB_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void SaveDate2DB(LikeSaveDBMessage dto, Channel channel,
                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始存库点赞记录");
            likeService.save2DB(dto);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("点赞存储失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        // 存库成功后，异步操作独立处理，失败不影响已 ACK 的消息
        if (dto.getIsLike()) {
            // 自己点赞自己的内容不发通知
            if (!dto.getAuthorId().equals(dto.getUserId())) {
                sendLikeNotification(dto);
            }
            // 给被点赞的作者发放积分（防重复由积分模块保证）
            try {
                pointMqApi.sendLikePoint(dto.getUserId(), dto.getAuthorId(), dto.getTargetId(), dto.getTargetType());
            } catch (Exception e) {
                log.error("发送点赞积分消息失败: targetId={}, authorId={}", dto.getTargetId(), dto.getAuthorId(), e);
            }
        }
    }

    /** 发送点赞通知 */
    private void sendLikeNotification(LikeSaveDBMessage dto) {
        try {
            List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(dto.getUserId()));
            UserDTO sender = users.isEmpty() ? null : users.getFirst();

            String notifyTitle = dto.getTargetTitle();
            String notifyContent = dto.getTargetContent();
            if (COMMENT.equalsIgnoreCase(dto.getTargetType())) {
                notifyTitle = dto.getTargetContent();
                notifyContent = null;
            }

            NotificationMessage messageDTO = NotificationMessage.builder()
                    .userId(dto.getAuthorId())
                    .actionType("like")
                    .targetType(dto.getTargetType().toLowerCase())
                    .targetId(dto.getTargetId())
                    .senderId(dto.getUserId())
                    .targetTitle(notifyTitle)
                    .content(notifyContent)
                    .relatedId(dto.getRelatedId())
                    .senderNickname(sender != null ? sender.getNickname() : "用户")
                    .senderAvatar(sender != null ? sender.getAvatar() : "")
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationApi.saveNotification(messageDTO);
            log.debug("发送点赞通知成功: targetId={}, userId={}", dto.getTargetId(), dto.getUserId());
        } catch (Exception e) {
            log.error("发送点赞通知失败, targetId={}, targetType={}, userId={}",
                    dto.getTargetId(), dto.getTargetType(), dto.getUserId(), e);
        }
    }

    @RabbitListener(queues = LIKE_SYNC_CACHE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void syncLikeCache(SyncLikeCacheMessage dto, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 同类型点赞缓存同步使用分布式锁串行执行，防止并发
        RLock lock = redissonClient.getLock(LIKE_SYNC_LOCK_PREFIX + dto.getTargetType() + ":" + dto.getBizId());
        try {
            boolean tryLock = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!tryLock) {
                log.warn("获取锁超时, targetType={}, 消息将重新入队", dto.getTargetType());
                channel.basicNack(deliveryTag, false, true);
                return;
            }
            log.info("开始同步点赞缓存, targetType={}", dto.getTargetType());
            LikeStrategy likeStrategy = likeStrategyMap.get(dto.getTargetType());
            if (likeStrategy != null) {
                likeStrategy.AllSync2Cache(dto.getBizId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("同步点赞缓存失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
