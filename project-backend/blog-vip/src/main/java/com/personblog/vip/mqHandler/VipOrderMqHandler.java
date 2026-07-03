package com.personblog.vip.mqHandler;

import com.personblog.vip.bizService.OrderBizService;
import com.personblog.vip.bizService.VipMqBizService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.personblog.vip.config.mqConfig.VipOrderMqConfig.*;

/**
 * VIP 订单 MQ 消费者（薄层，仅负责 ack/nack，业务逻辑委托 BizService）
 *
 * @author LSH
 * @since 2026-06-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipOrderMqHandler {

    private final VipMqBizService vipMqBizService;
    private final OrderBizService orderBizService;
    private final RabbitTemplate rabbitTemplate;

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
     * TCC Confirm 重试消费者（指数退避 + 死信兜底）
     * <pre>
     * 重试流程：
     *   1. 消费重试队列消息
     *   2. 执行 TCC Confirm（积分扣减 + 优惠券核销）
     *   3. 成功 → ACK
     *   4. 失败 + 未达重试上限 → 发到延迟队列（TTL = 基础延迟 * 2^retryCount），ACK 当前消息
     *   5. 失败 + 已达重试上限 → NACK，进入死信队列
     * </pre>
     */
    @RabbitListener(queues = TCC_CONFIRM_RETRY_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void handleTccConfirmRetry(String orderIdStr, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                      Message message) throws IOException {
        Long orderId = Long.parseLong(orderIdStr);
        MessageProperties props = message.getMessageProperties();
        int retryCount = getRetryCount(props);

        try {
            boolean success = orderBizService.handleTccConfirmRetry(orderId);
            if (success) {
                channel.basicAck(deliveryTag, false);
                log.info("TCC Confirm重试成功: orderId={}, retryCount={}", orderId, retryCount);
            } else if (retryCount < TCC_CONFIRM_MAX_RETRIES) {
                // 未达上限：发到延迟队列做指数退避，ACK 当前消息
                long delayMs = TCC_CONFIRM_BASE_DELAY_MS * (1L << retryCount); // 1s, 2s, 4s, 8s...
                publishToDelayQueue(orderIdStr, retryCount + 1, delayMs);
                channel.basicAck(deliveryTag, false);
                log.info("TCC Confirm重试失败，已发延迟队列: orderId={}, nextRetry={}, delayMs={}",
                        orderId, retryCount + 1, delayMs);
            } else {
                // 已达上限：NACK → 死信队列
                channel.basicNack(deliveryTag, false, false);
                log.error("TCC Confirm重试耗尽: orderId={}, retryCount={}, 已进入死信队列",
                        orderId, retryCount);
            }
        } catch (Exception e) {
            log.error("TCC Confirm重试异常: orderId={}", orderId, e);
            if (retryCount < TCC_CONFIRM_MAX_RETRIES) {
                long delayMs = TCC_CONFIRM_BASE_DELAY_MS * (1L << retryCount);
                publishToDelayQueue(orderIdStr, retryCount + 1, delayMs);
                channel.basicAck(deliveryTag, false);
            } else {
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    /**
     * 从消息属性中获取重试次数（默认 0）
     */
    private int getRetryCount(MessageProperties props) {
        Object count = props.getHeader("x-retry-count");
        if (count instanceof Integer i) {
            return i;
        }
        if (count instanceof Long l) {
            return l.intValue();
        }
        return 0;
    }

    /**
     * 发布消息到延迟队列，设置 TTL 实现指数退避
     */
    private void publishToDelayQueue(String orderIdStr, int retryCount, long delayMs) {
        rabbitTemplate.convertAndSend(
                VIP_ORDER_EXCHANGE,
                TCC_CONFIRM_RETRY_DELAY_KEY,
                orderIdStr,
                msg -> {
                    msg.getMessageProperties().setHeader("x-retry-count", retryCount);
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return msg;
                }
        );
    }
}
