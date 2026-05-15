package com.personblog.interaction.mqHandler;

import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Notification.sse.NotificationMessageDTO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.interaction.dto.MqMessage.FollowMessageDTO;
import com.personblog.interaction.entity.BizNotification;
import com.personblog.interaction.mapper.BizNotificationMapper;
import com.personblog.push.service.OnlineStateService;
import com.personblog.push.sse.SseEmitterManager;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.personblog.common.config.mqConfig.InteractionMqConfig.FOLLOW_NOTIFICATION_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMqHandler {

    private final BizNotificationMapper bizNotificationMapper;
    private final SseEmitterManager sseEmitterManager;
    private final OnlineStateService onlineStateService; // 复用 WebSocket 的在线状态服务
    private final UseApi useApi;

    /**
     * 处理关注通知
     * 监听关注通知队列，保存通知并推送
     */
    @RabbitListener(queues = FOLLOW_NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleFollowNotification(FollowMessageDTO dto, Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到关注消息: followerId={}, followingId={}", dto.getFollowerId(), dto.getFollowingId());

            // 只处理关注操作，不处理取关
            if (dto.getIsFollow()) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 保存通知到数据库
            BizNotification notification = new BizNotification();
            notification.setUserId(dto.getFollowingId()); // 接收通知的用户（被关注者）
            notification.setActionType("follow");
            notification.setTargetType("user");
            notification.setTargetId(dto.getFollowerId()); // 目标ID是关注者ID
            notification.setSenderId(dto.getFollowerId()); // 发送者是关注者
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            bizNotificationMapper.insert(notification);
            log.info("保存关注通知成功: notificationId={}", notification.getId());

            // 检查用户是否在线（复用 WebSocket 的在线状态判断）
            boolean isOnline = onlineStateService.isOnline(dto.getFollowingId());

            if (isOnline) {
                // 查询发送者信息
                List<UserDTO> users = useApi.getUserInfo(Collections.singletonList(dto.getFollowerId()));
                UserDTO sender = users.isEmpty() ? null : users.getFirst();

                // 构建推送消息
                NotificationMessageDTO message = NotificationMessageDTO.builder()
                    .id(notification.getId())
                    .actionType("follow")
                    .targetType("user")
                    .targetId(dto.getFollowerId())
                    .senderId(dto.getFollowerId())
                    .senderNickname(sender != null ? sender.getNickname() : "用户")
                    .senderAvatar(sender != null ? sender.getAvatar() : "")
                    .createdAt(notification.getCreatedAt())
                    .build();

                // 推送消息
                sseEmitterManager.sendToUser(dto.getFollowingId(), message);
                log.info("推送关注通知成功: userId={}", dto.getFollowingId());
            } else {
                log.debug("用户 {} 不在线，不推送通知", dto.getFollowingId());
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("关注通知处理失败，消息将转入死信队列: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
