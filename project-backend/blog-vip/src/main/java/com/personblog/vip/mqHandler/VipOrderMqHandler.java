package com.personblog.vip.mqHandler;

import com.personblog.vip.bizService.VipMqBizService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.vip.config.mqConfig.VipOrderMqConfig.ORDER_TIMEOUT_QUEUE;

/**
 * VIP 订单 MQ 消费者（薄层，仅负责 ack/nack，业务逻辑委托 MqBizService）
 *
 * @author LSH
 * @since 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipOrderMqHandler {

    private final VipMqBizService vipMqBizService;

    /**
     * 订单超时处理
     */
    @RabbitListener(queues = ORDER_TIMEOUT_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderTimeout(String orderIdStr, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            vipMqBizService.handleOrderTimeout(Long.parseLong(orderIdStr));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("订单超时处理异常: orderIdStr={}", orderIdStr, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
