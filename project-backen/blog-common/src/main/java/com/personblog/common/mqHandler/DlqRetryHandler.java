package com.personblog.common.mqHandler;

import com.personblog.common.entity.MqErrorLog;
import com.personblog.common.service.IMqErrorLogService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.personblog.common.config.mqConfig.AiMqConfig.AI_TITLE_DLQ;
import static com.personblog.common.config.mqConfig.ArticleMqConfig.ARTICLE_ES_DLQ;
import static com.personblog.common.config.mqConfig.CommentMqConfig.AI_COMMENT_DLQ;
import static com.personblog.common.config.mqConfig.CommentMqConfig.COMMENT_NOTIFICATION_DLQ;
import static com.personblog.common.config.mqConfig.InteractionMqConfig.*;
import static com.personblog.common.constant.StatusConstant.PENDING;

/**
 * 死信队列（DLQ）重试处理器
 * <p>
 * 当消息从核心业务队列进入死信队列后，该处理器负责：
 * 1. 提取消息中的重试次数，若未超过上限则延迟重新投递到原交换机
 * 2. 若超过重试上限，则将消息记录到 tb_mq_error_log 表，供人工排查
 * </p>
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqRetryHandler {

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    /** 重试延迟基数（秒），每次重试延迟 = baseDelaySec * retryCount */
    private static final int BASE_DELAY_SEC = 1;

    private final RabbitTemplate rabbitTemplate;
    private final IMqErrorLogService errorLogService;

    /**
     * 监听所有核心死信队列，处理死信消息
     */
    @RabbitListener(queues = {
            LIKE_DLQ, LIKE_DB_DLQ,
            FOLLOW_DLQ, FOLLOW_NOTIFICATION_DLQ,
            USER_LIKE_DLQ, COLLECTION_DLQ, BROWSE_HISTORY_DLQ,
            AI_COMMENT_DLQ, COMMENT_NOTIFICATION_DLQ, AI_TITLE_DLQ,
            ARTICLE_ES_DLQ
    }, containerFactory = "rabbitListenerContainerFactory")
    public void handleDeadLetter(Message message, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 提取消息元数据
        String originalExchange = extractOriginalExchange(message);
        String originalRoutingKey = extractOriginalRoutingKey(message);
        int retryCount = extractRetryCount(message);
        String queueName = extractOriginalQueue(message);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.warn("收到死信消息，队列: {}, 已重试次数: {}/{}, 交换机: {}, 路由键: {}",
                queueName, retryCount, MAX_RETRY_COUNT, originalExchange, originalRoutingKey);

        try {
            if (retryCount < MAX_RETRY_COUNT) {
                // 未超过最大重试次数，递增重试计数并异步重新投递
                message.getMessageProperties().setHeader("x-retry-count", retryCount + 1);
                int delaySec = BASE_DELAY_SEC * (retryCount + 1);
                asyncRetry(message, originalExchange, originalRoutingKey, delaySec);
                channel.basicAck(deliveryTag, false);
                log.info("死信消息重新投递，目标交换机: {}, 路由键: {}, 延迟: {}秒, 当前重试次数: {}",
                        originalExchange, originalRoutingKey, delaySec, retryCount + 1);
            } else {
                // 超过最大重试次数，记录到错误日志表
                saveErrorLog(queueName, originalExchange, originalRoutingKey, body, retryCount);
                channel.basicAck(deliveryTag, false);
                log.error("死信消息超过最大重试次数({})，已记录到 tb_mq_error_log，队列: {}", MAX_RETRY_COUNT, queueName);
            }
        } catch (Exception e) {
            log.error("处理死信消息异常: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 异步重新投递消息，避免阻塞 DLQ 消费者线程
     *
     * @param message  原始消息
     * @param exchange 目标交换机
     * @param routingKey 目标路由键
     * @param delaySec 延迟秒数
     */
    @Async("DlqExecutor")
    public void asyncRetry(Message message, String exchange, String routingKey, int delaySec) {
        try {
            Thread.sleep(delaySec * 1000L);
            rabbitTemplate.send(exchange, routingKey, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("死信重试延迟被中断", e);
        }
    }

    /**
     * 记录超过重试上限的死信消息到 tb_mq_error_log
     */
    private void saveErrorLog(String queueName, String exchange, String routingKey,
                              String messageBody, int retryCount) {
        try {
            MqErrorLog errorLog = new MqErrorLog();
            errorLog.setQueueName(queueName);
            errorLog.setExchangeName(exchange);
            errorLog.setRoutingKey(routingKey);
            errorLog.setMessageBody(messageBody);
            errorLog.setRetryCount(retryCount);
            errorLog.setMaxRetryCount(MAX_RETRY_COUNT);
            errorLog.setStatus(PENDING);
            errorLog.setCreatedAt(LocalDateTime.now());
            errorLog.setUpdatedAt(LocalDateTime.now());
            errorLogService.save(errorLog);
        } catch (Exception e) {
            log.error("保存死信消息到 tb_mq_error_log 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 x-death 头中提取原始交换机名称
     */
    private String extractOriginalExchange(Message message) {
        Object xDeathObj = message.getMessageProperties().getHeader("x-death");
        if (xDeathObj instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
            Object first = xDeathList.getFirst();
            if (first instanceof Map<?, ?> map) {
                Object exchange = map.get("exchange");
                return exchange instanceof String s ? s : "unknown_exchange";
            }
        }
        return "unknown_exchange";
    }

    /**
     * 从 x-death 头中提取原始路由键
     */
    private String extractOriginalRoutingKey(Message message) {
        Object xDeathObj = message.getMessageProperties().getHeader("x-death");
        if (xDeathObj instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
            Object first = xDeathList.getFirst();
            if (first instanceof Map<?, ?> map) {
                Object routingKeysObj = map.get("routing-keys");
                if (routingKeysObj instanceof List<?> routingKeys && !routingKeys.isEmpty()) {
                    Object rk = routingKeys.getFirst();
                    return rk instanceof String s ? s : "unknown_routing_key";
                }
            }
        }
        return "unknown_routing_key";
    }

    /**
     * 从 x-death 头中提取原始队列名称
     */
    private String extractOriginalQueue(Message message) {
        Object xDeathObj = message.getMessageProperties().getHeader("x-death");
        if (xDeathObj instanceof List<?> xDeathList && !xDeathList.isEmpty()) {
            Object first = xDeathList.getFirst();
            if (first instanceof Map<?, ?> map) {
                Object queue = map.get("queue");
                return queue instanceof String s ? s : "unknown_queue";
            }
        }
        return "unknown_queue";
    }

    /**
     * 提取自定义重试次数头，如果不存在则返回0
     */
    private int extractRetryCount(Message message) {
        Object retryObj = message.getMessageProperties().getHeader("x-retry-count");
        if (retryObj instanceof Integer count) {
            return count;
        }
        return 0;
    }
}
