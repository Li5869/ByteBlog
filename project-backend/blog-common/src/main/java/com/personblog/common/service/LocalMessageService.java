package com.personblog.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.entity.LocalMessage;

/**
 * 本地消息表 Service 接口
 *
 * @author LSH
 * @since 2026-06-05
 */
public interface LocalMessageService extends IService<LocalMessage> {
    /**
     * 尝试发送已持久化的消息到 MQ
     * 发送成功则更新状态为已完成，失败则保留待发送状态等待定时任务补偿
     *
     * @param message 已持久化的本地消息
     */
    void trySend(LocalMessage message);

    /**
     * 重试待处理消息（定时任务调用）
     * 1. 查询待发送且到达重试时间的消息
     * 2. 尝试发送 MQ
     * 3. 失败则更新重试次数和下次重试时间
     */
    void retryPendingMessages();
}
