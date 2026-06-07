package com.personblog.vip.mqHandler;

import cn.hutool.json.JSONUtil;
import com.personblog.common.dto.MqMessage.Vip.OrderConfirmMessageDTO;
import com.personblog.vip.bizService.VipMqBizService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.vip.config.mqConfig.VipOrderMqConfig.ORDER_CONFIRM_QUEUE;
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

    /**
     * 订单确认处理（支付成功后激活会员）
     * 接收 String 类型消息，手动解析为对象（与优惠券消费者保持一致）
     */
    @RabbitListener(queues = ORDER_CONFIRM_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleOrderConfirm(String messageBody, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 手动解析 JSON 字符串为对象
        OrderConfirmMessageDTO msg = JSONUtil.toBean(messageBody, OrderConfirmMessageDTO.class);

        try {
            vipMqBizService.handleOrderConfirm(msg);
            channel.basicAck(deliveryTag, false);
            log.info("订单确认处理成功: orderId={}, userId={}", msg.getOrderId(), msg.getUserId());
        } catch (Exception e) {
            log.error("订单确认处理异常: orderId={}", msg.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
