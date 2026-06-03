package com.personblog.point.apiimpl;

import com.personblog.api.pointAPI.PointMqApi;
import com.personblog.common.dto.MqMessage.Point.PointMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;
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

    /** 互动目标类型 → 积分类型映射 */
    private static final Map<String, String> TARGET_TO_POINT_TYPE = Map.of(
            ARTICLE, ARTICLE_LIKED,
            COMMENT, COMMENT_LIKED
    );

    @Override
    public void sendArticlePoint(Long userId, Long articleId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .authorId(userId)
                .points(20)
                .type(ARTICLE_PUBLISHED)
                .bizId(articleId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_ARTICLE_KEY, message);
        log.debug("发送文章发布积分消息: userId={}, articleId={}", userId, articleId);
    }

    @Override
    public void sendLikePoint(Long likerId, Long authorId, Long bizId, String targetType) {
        // 将互动类型（article/comment）映射为积分类型（article_liked/comment_liked）
        String pointType = TARGET_TO_POINT_TYPE.get(targetType.toLowerCase());
        if (pointType == null) {
            log.warn("未知的点赞目标类型，跳过积分发放: targetType={}", targetType);
            return;
        }
        // userId = 积分接收者（作者），operatorId = 点赞者（用于防重复去重）
        PointMessageDTO message = PointMessageDTO.builder()
                .authorId(authorId)
                .points(2)
                .type(pointType)
                .bizId(bizId)
                .operatorId(likerId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_LIKE_KEY, message);
        log.debug("发送点赞积分消息: likerId={}, authorId={}, bizId={}", likerId, authorId, bizId);
    }

    @Override
    public void sendCollectionPoint(Long operatorId,Long authorId, Long articleId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .authorId(authorId)
                .operatorId(operatorId)
                .points(3)
                .type(ARTICLE_COLLECTED)
                .bizId(articleId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_COLLECTION_KEY, message);
        log.debug("发送收藏积分消息: userId={}, articleId={}", authorId, articleId);
    }

    @Override
    public void sendAdminAdjustPoint(Long userId, Integer points, String description, Long operatorId) {
        PointMessageDTO message = PointMessageDTO.builder()
                .authorId(userId)
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
