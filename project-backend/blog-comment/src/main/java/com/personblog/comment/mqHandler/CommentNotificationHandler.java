package com.personblog.comment.mqHandler;

import com.personblog.api.articleAPI.ArticleAPI;
import com.personblog.api.articleAPI.ArticleMqAPI;
import com.personblog.api.interactionAPI.NotificationApi;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.comment.entity.Comment;
import com.personblog.comment.service.ICommentService;
import com.personblog.common.dto.MqMessage.AIModerate.AiModerateMessage;
import com.personblog.common.dto.MqMessage.Comment.CommentNotificationMessage;
import com.personblog.common.dto.MqMessage.notifaction.NotificationMessage;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.push.sse.SseEmitterManager;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.personblog.comment.config.mqConfig.CommentMqConfig.COMMENT_NOTIFICATION_QUEUE;
import static com.personblog.common.constant.MqRoutingConstants.AI_EXCHANGE;
import static com.personblog.common.constant.MqRoutingConstants.AI_MODERATE_KEY;
import static com.personblog.common.constant.TargetTypeConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationHandler {

    private final ICommentService commentService;
    private final UseApi useApi;
    private final ArticleMqAPI articleMqAPI;
    private final ArticleAPI articleAPI;
    private final NotificationApi notificationApi;
    private final SseEmitterManager sseEmitterManager;
    private final RabbitTemplate rabbitTemplate;
    @RabbitListener(queues = COMMENT_NOTIFICATION_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleCommentNotification(CommentNotificationMessage message,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            // 1. 更新文章评论数
            if (message.getDelta() != null) {
                articleMqAPI.updateCommentCount(message.getArticleId(), message.getDelta());
                log.debug("文章评论数已更新, articleId={}, delta={}", message.getArticleId(), message.getDelta());
            }

            // 2. 发送评论通知（删除评论等纯统计场景无需通知）
            if (message.getUserId() == null) {
                channel.basicAck(deliveryTag, false);
                log.info("评论统计更新处理完成, articleId={}, delta={}", message.getArticleId(), message.getDelta());
                return;
            }

            // 获取文章作者 ID
            Long authorId = articleAPI.getArticleAuthorId(message.getArticleId());
            //发送ai审核消息
            AiModerateMessage aiModerateMessage = new AiModerateMessage();
            aiModerateMessage.setAuthorId(message.getUserId());
            aiModerateMessage.setContent(message.getContent());
            aiModerateMessage.setBizId(message.getCommentId());
            aiModerateMessage.setBizType(COMMENT);
            rabbitTemplate.convertAndSend(AI_EXCHANGE, AI_MODERATE_KEY, aiModerateMessage);
            // 获取评论者信息
            List<UserDTO> users = useApi.getUserInfo(Collections.singleton(message.getUserId()));
            UserDTO sender = users.isEmpty() ? null : users.getFirst();

            // 确定通知接收者、动作类型和目标类型
            Long receiverId;
            String actionType;
            String targetType;
            String parentCommentContent = null;
            if (message.getParentId() != null) {
                // 回复评论：通知被回复的评论作者
                Comment parentComment = commentService.getById(message.getParentId());
                receiverId = parentComment.getAuthorId();
                parentCommentContent = parentComment.getContent();
                actionType = REPLY;
                targetType = COMMENT;
            } else {
                // 评论文章：通知文章作者
                receiverId = authorId;
                actionType = COMMENT;
                targetType = ARTICLE;
            }

            // 不给自己发通知
            if (!receiverId.equals(message.getUserId())) {
                Long relatedId;
                Long targetId;
                if (message.getParentId() != null) {
                    targetId = message.getParentId();
                    relatedId = message.getArticleId();
                } else {
                    targetId = message.getArticleId();
                    relatedId = message.getCommentId();
                }

                NotificationMessage notificationDTO = NotificationMessage.builder()
                        .userId(receiverId)
                        .actionType(actionType)
                        .targetType(targetType)
                        .targetId(targetId)
                        .senderId(message.getUserId())
                        .targetTitle(parentCommentContent)
                        .targetTitle(message.getArticleTitle())
                        .relatedId(relatedId)
                        .senderNickname(sender != null ? sender.getNickname() : "用户")
                        .senderAvatar(sender != null ? sender.getAvatar() : "")
                        .content(message.getContent())
                        .createdAt(LocalDateTime.now())
                        .build();

                // 保存通知到数据库
                Long notificationId = notificationApi.saveNotification(notificationDTO);

                // 设置通知 ID 后发送 SSE 实时通知
                if (notificationId != null) {
                    notificationDTO.setId(notificationId);
                    sseEmitterManager.sendToUser(receiverId, notificationDTO);
                }
            }

            // 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("评论通知处理成功, commentId={}, articleId={}", message.getCommentId(), message.getArticleId());

        } catch (Exception e) {
            log.error("评论通知处理失败, commentId={}, articleId={}", message.getCommentId(), message.getArticleId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
