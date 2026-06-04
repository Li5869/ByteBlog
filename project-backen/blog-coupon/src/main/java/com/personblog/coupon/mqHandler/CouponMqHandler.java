package com.personblog.coupon.mqHandler;

import com.personblog.common.dto.MqMessage.Coupon.CouponClaimMessageDTO;
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
 * 处理优惠券领取等消息（骨架，后续实现业务逻辑）
 *
 * @author LSH
 * @since 2026-06-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponMqHandler {

    /**
     * 处理优惠券领取消息
     */
    @RabbitListener(queues = COUPON_CLAIM_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleCouponClaim(CouponClaimMessageDTO message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("收到优惠券领取消息: userId={}, couponTemplateId={}", message.getUserId(), message.getCouponTemplateId());
            // TODO 后续实现优惠券入库逻辑
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("优惠券领取处理失败: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
