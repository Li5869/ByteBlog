package com.personblog.ai.api;

import com.personblog.ai.BizService.AIMemoryService;
import com.personblog.api.AIAPI.MemoryExtractApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 记忆提取 API 实现
 * 实现 blog-api 中定义的 MemoryExtractApi 接口，委托给 AIMemoryService 处理
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryExtractApiImpl implements MemoryExtractApi {

    private final AIMemoryService aiMemoryService;

    @Override
    public void extractMemories() {
        log.info("[记忆提取] 收到提取请求");
        aiMemoryService.extractMemories();
    }
}
