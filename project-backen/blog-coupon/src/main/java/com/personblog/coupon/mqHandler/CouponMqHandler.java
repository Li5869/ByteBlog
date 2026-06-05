package com.personblog.coupon.mqHandler;

import com.personblog.common.dto.MqMessage.Coupon.CouponClaimMessageDTO;
import com.personblog.coupon.bizService.MqBizService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.coupon.config.mqConfig.CouponMqConfig.COUPON_CLAIM_QUEUE;

/**
 * 优惠券系统 MQ 消息处理器
 *
 * @author LSH
 * @since 2026-06-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponMqHandler {

    private final MqBizService mqBizService;

    /**
     * 处理优惠券领取消息
     * 成功 → ACK（消息从队列移除）
     * 失败 → NACK + 不重回队列（进入死信队列 coupon_claim_dlq）
     */
    @RabbitListener(queues = COUPON_CLAIM_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleCouponClaim(CouponClaimMessageDTO message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到优惠券领取消息: userId={}, couponTemplateId={}",
                    message.getUserId(), message.getCouponTemplateId());

            mqBizService.handleCouponClaim(message);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("优惠券领取处理失败，消息进入死信队列: userId={}, couponTemplateId={}, error={}",
                    message.getUserId(), message.getCouponTemplateId(), e.getMessage(), e);
            //回滚积分
            mqBizService.cancelPoint(message.getUserId(),message.getPointsCost());
            // 不重回队列，进入死信队列等待人工处理
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
