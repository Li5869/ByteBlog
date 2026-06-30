package com.personblog.api.AIAPI;

/**
 * 记忆提取 API 接口
 * 用于跨模块调度记忆提取任务（blog-job → blog-ai）
 *
 * @author LSH
 */
public interface MemoryExtractApi {

    /**
     * 扫描 Redis 中过期的对话活跃标记，调用 Python 端提取记忆
     * 由 XXL-Job 定时触发，每 3 分钟执行一次
     */
    void extractMemories();
}
