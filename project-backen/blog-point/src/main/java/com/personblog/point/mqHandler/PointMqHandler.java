package com.personblog.point.mqHandler;

import com.personblog.common.dto.MqMessage.Point.PointMessageDTO;
import com.personblog.point.BizService.PointBizService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.point.config.mqConfig.PointMqConfig.*;

/**
 * 积分系统 MQ 消息处理器
 * 处理积分发放、扣减等消息
 *
 * @author LSH
 * @since 2026-06-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointMqHandler {

    private final PointBizService pointBizService;

    /**
     * 处理签到积分发放消息
     */
    @RabbitListener(queues = POINT_SIGN_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleSignPoint(PointMessageDTO message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到签到积分消息: userId={}, points={}", message.getUserId(), message.getPoints());
            pointBizService.changePoints(
                    message.getUserId(),
                    message.getPoints(),
                    message.getType(),
                    null,
                    null
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("签到积分处理失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理文章积分发放消息（发布文章、被点赞、被收藏）
     */
    @RabbitListener(queues = {POINT_ARTICLE_QUEUE, POINT_LIKE_QUEUE, POINT_COLLECTION_QUEUE},
            containerFactory = "rabbitListenerContainerFactory")
    public void handleArticlePoint(PointMessageDTO message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到文章积分消息: userId={}, points={}, type={}", message.getUserId(), message.getPoints(), message.getType());
            pointBizService.changePoints(
                    message.getUserId(),
                    message.getPoints(),
                    message.getType(),
                    message.getBizId(),
                    null
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("文章积分处理失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 处理管理员调整积分消息
     */
    @RabbitListener(queues = POINT_ADMIN_ADJUST_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleAdminAdjustPoint(PointMessageDTO message, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到管理员调整积分消息: userId={}, points={}, operatorId={}", message.getUserId(), message.getPoints(), message.getOperatorId());
            pointBizService.changePoints(
                    message.getUserId(),
                    message.getPoints(),
                    message.getType(),
                    null,
                    message.getDescription()
            );
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("管理员调整积分处理失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
