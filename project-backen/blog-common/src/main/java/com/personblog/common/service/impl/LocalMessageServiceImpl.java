package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.entity.LocalMessage;
import com.personblog.common.mapper.LocalMessageMapper;
import com.personblog.common.service.LocalMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地消息表 Service 实现
 *
 * @author LSH
 * @since 2026-06-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalMessageServiceImpl extends ServiceImpl<LocalMessageMapper, LocalMessage>
        implements LocalMessageService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 消息状态常量
     */
    private static final int STATUS_PENDING = 0;    // 待发送
    private static final int STATUS_SUCCESS = 1;    // 已完成（发送成功）
    private static final int STATUS_FAILED = 2;     // 已失败（超过重试次数）

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRY = 5;

    @Override
    public void saveAndTrySend(LocalMessage message) {
        // 1. 初始化消息状态
        if (message.getStatus() == null) {
            message.setStatus(STATUS_PENDING);
        }
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }
        if (message.getMaxRetry() == null) {
            message.setMaxRetry(DEFAULT_MAX_RETRY);
        }
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        // 2. 写入消息表
        this.save(message);

        // 3. 立即尝试发送
        trySend(message);
    }

    @Override
    public void trySend(LocalMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    message.getExchange(),
                    message.getRoutingKey(),
                    message.getMessageBody()
            );
            // 发送成功，更新状态为已完成
            message.setStatus(STATUS_SUCCESS);
            message.setUpdatedAt(LocalDateTime.now());
            this.updateById(message);
            log.info("本地消息发送成功: bizType={}, bizId={}", message.getBizType(), message.getBizId());
        } catch (Exception e) {
            // 发送失败，不更新状态，等定时任务补偿
            log.warn("本地消息发送失败，等待定时补偿: bizType={}, bizId={}, error={}",
                    message.getBizType(), message.getBizId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public void retryPendingMessages() {
        // 1. 查询待发送且到达重试时间的消息
        List<LocalMessage> messages = this.lambdaQuery()
                .eq(LocalMessage::getStatus, STATUS_PENDING)
                .le(LocalMessage::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(LocalMessage::getCreatedAt)
                .last("LIMIT 100")
                .list();

        if (messages.isEmpty()) {
            return;
        }

        log.info("开始重试本地消息，待处理数量: {}", messages.size());

        // 2. 逐个发送
        List<LocalMessage> failedMessages = new ArrayList<>();
        int successCount = 0;

        for (LocalMessage message : messages) {
            // 乐观锁：尝试更新状态，防止重复发送
            boolean updated = this.lambdaUpdate()
                    .eq(LocalMessage::getId, message.getId())
                    .eq(LocalMessage::getStatus, STATUS_PENDING)
                    .set(LocalMessage::getStatus, STATUS_SUCCESS)
                    .set(LocalMessage::getUpdatedAt, LocalDateTime.now())
                    .update();

            // 更新失败说明被其他线程处理，跳过
            if (!updated) {
                continue;
            }

            try {
                rabbitTemplate.convertAndSend(
                        message.getExchange(),
                        message.getRoutingKey(),
                        message.getMessageBody()
                );
                successCount++;
                log.debug("本地消息重试发送成功: bizType={}, bizId={}", message.getBizType(), message.getBizId());
            } catch (Exception e) {
                // 发送失败，记录待更新
                message.setStatus(STATUS_PENDING);
                message.setRetryCount(message.getRetryCount() + 1);
                message.setUpdatedAt(LocalDateTime.now());

                if (message.getRetryCount() >= message.getMaxRetry()) {
                    // 超过最大重试次数，标记为失败
                    message.setStatus(STATUS_FAILED);
                    log.error("本地消息发送失败，超过最大重试次数: bizType={}, bizId={}, retryCount={}",
                            message.getBizType(), message.getBizId(), message.getRetryCount());
                } else {
                    // 指数退避：2^retryCount 秒后重试
                    LocalDateTime nextRetry = LocalDateTime.now()
                            .plusSeconds((long) Math.pow(2, message.getRetryCount()));
                    message.setNextRetryTime(nextRetry);
                    log.warn("本地消息重试发送失败，{}秒后重试: bizType={}, bizId={}, retryCount={}",
                            (long) Math.pow(2, message.getRetryCount()),
                            message.getBizType(), message.getBizId(), message.getRetryCount());
                }
                failedMessages.add(message);
            }
        }

        // 3. 批量更新失败消息
        if (!failedMessages.isEmpty()) {
            this.updateBatchById(failedMessages);
        }

        log.info("本地消息重试完成: 成功={}, 失败={}", successCount, failedMessages.size());
    }
}
