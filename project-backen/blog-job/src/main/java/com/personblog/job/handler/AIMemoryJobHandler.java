package com.personblog.job.handler;

import com.personblog.api.AIAPI.MemoryExtractApi;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 记忆提取定时任务处理器
 * 每 3 分钟扫描 Redis 中过期的对话活跃标记，调用 Python 端提取长期记忆
 *
 * @author LSH
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AIMemoryJobHandler {

    private final MemoryExtractApi memoryExtractApi;

    /**
     * 扫描并调度记忆提取
     * XXL-Job 配置：Cron = 0 * /3 * * * ? （每3分钟）
     */
    @XxlJob("memoryExtractJobHandler")
    public void memoryExtract() {
        log.info("[记忆提取] 定时任务触发");
        memoryExtractApi.extractMemories();
        log.info("[记忆提取] 定时任务完成");
    }
}
