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
}
