package com.personblog.job.handler;

import com.personblog.common.service.LocalMessageService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 本地消息表定时任务处理器
 * 负责重试发送失败的 MQ 消息，保证消息可靠投递
 *
 * @author LSH
 * @since 2026-06-05
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LocalMessageJobHandler {

    private final LocalMessageService localMessageService;

//     * 重试待发送的本地消息
//     * 建议配置：每 5 秒执行一次（*/5 * * * * ?）
//     *
//     * 处理逻辑：
//     * 1. 查询状态为"待发送"且到达重试时间的消息
//     * 2. 尝试发送 MQ
//     * 3. 发送成功则更新状态为"已完成"
//     * 4. 发送失败则更新重试次数，使用指数退避策略
//     * 5. 超过最大重试次数则标记为"已失败"
//     *
    @XxlJob("retryLocalMessage")
    public void retryLocalMessage() {
        log.info("开始执行本地消息重试任务");
        try {
            localMessageService.retryPendingMessages();
            log.info("本地消息重试任务执行完成");
        } catch (Exception e) {
            log.error("本地消息重试任务执行异常", e);
        }
    }

    /**
     * 清理过期的已发送消息（可选）
     * 建议配置：每天凌晨 2 点执行（0 0 2 * * ?）
     * 处理逻辑：
     * 1. 删除状态为"已完成"且创建时间超过 7 天的消息
     * 2. 释放数据库空间
     */
    @XxlJob("cleanExpiredLocalMessage")
    public void cleanExpiredLocalMessage() {
        log.info("开始执行过期本地消息清理任务");
        try {
            // 删除 7 天前已完成的消息
            boolean removed = localMessageService.lambdaUpdate()
                    .eq(com.personblog.common.entity.LocalMessage::getStatus, 1) // 已完成
                    .lt(com.personblog.common.entity.LocalMessage::getCreatedAt,
                            java.time.LocalDateTime.now().minusDays(7))
                    .remove();

            log.info("过期本地消息清理任务执行完成, 删除结果: {}", removed);
        } catch (Exception e) {
            log.error("过期本地消息清理任务执行异常", e);
        }
    }
}
