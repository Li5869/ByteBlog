package com.personblog.point.apiimpl;

import com.personblog.api.pointAPI.PointMqApi;
import com.personblog.common.dto.MqMessage.Point.PointMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.personblog.point.config.mqConfig.PointMqConfig.*;
import static com.personblog.point.constant.PointTypeConstants.*;

/**
 * 积分消息发送 API 实现
 * 封装 MQ 发送细节，供其他模块调用
 *
 * @author LSH
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointMqApiImpl implements PointMqApi {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendArticlePoint(Long userId, Long articleId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .userId(userId)
                .points(20)
                .type(ARTICLE_PUBLISHED)
                .bizId(articleId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_ARTICLE_KEY, message);
        log.debug("发送文章发布积分消息: userId={}, articleId={}", userId, articleId);
    }

    @Override
    public void sendLikePoint(Long userId, Long bizId, String bizType) {
        PointMessageDTO message = PointMessageDTO.builder()
                .userId(userId)
                .points(2)
                .type(bizType)
                .bizId(bizId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_LIKE_KEY, message);
        log.debug("发送点赞积分消息: userId={}, bizId={}, bizType={}", userId, bizId, bizType);
    }

    @Override
    public void sendCollectionPoint(Long userId, Long articleId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .userId(userId)
                .points(3)
                .type(ARTICLE_COLLECTED)
                .bizId(articleId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_COLLECTION_KEY, message);
        log.debug("发送收藏积分消息: userId={}, articleId={}", userId, articleId);
    }

    @Override
    public void sendAdminAdjustPoint(Long userId, Integer points, String description, Long operatorId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .userId(userId)
                .points(points)
                .type(ADMIN_ADJUST)
                .description(description)
                .operatorId(operatorId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_ADMIN_ADJUST_KEY, message);
        log.debug("发送管理员调整积分消息: userId={}, points={}, operatorId={}", userId, points, operatorId);
    }
}
