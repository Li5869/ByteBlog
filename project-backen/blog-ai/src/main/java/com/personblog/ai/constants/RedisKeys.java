package com.personblog.ai.constants;

public class RedisKeys {
    /**
     * AI 停止对话标志 Key 前缀
     * 完整 Key 格式：ai:chat:stop:{sessionId}
     * 存储内容：停止标志（用于中断 AI 对话）
     * 数据类型：String
     */
    public static final String REDIS_KEY_PREFIX = "ai:chat:stop:";

    /**
     * AI 对话记忆 Key 前缀
     * 完整 Key 格式：chat:memory:{sessionId}
     * 存储内容：AI 对话的上下文记忆
     * 数据类型：List/String
     */
    public static final String REDIS_MEMORY_PREFIX = "chat:memory:";

    /**
     * 对话活跃标记 Key 前缀（长期记忆提取用）
     * 完整 Key 格式：chat:active:{conversationId}
     * 存储内容：{"user_id": "...", "last_active": "2026-06-14T10:30:00"}
     * 数据类型：String (JSON)
     * TTL: 300s (Python 端设置，大于 XXL-Job 扫描间隔，防止误删)
     */
    public static final String REDIS_CHAT_ACTIVE_PREFIX = "chat:active:";
}
