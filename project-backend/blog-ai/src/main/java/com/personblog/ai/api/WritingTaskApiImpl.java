package com.personblog.ai.api;

import com.personblog.ai.service.IWritingTaskService;
import com.personblog.api.AIwritingAPI.WritingTaskApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 写作任务 API 实现
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WritingTaskApiImpl implements WritingTaskApi {

    private final IWritingTaskService writingTaskService;

    @Override
    public void completeTask(Long taskId, Long articleId, String finalAction) {
        if (taskId == null) {
            return;
        }
        // 更新文章ID
        writingTaskService.updateArticleId(taskId, articleId);
        // 完成任务（设置状态和最终动作）
        writingTaskService.completeTask(taskId, finalAction);
        log.info("[WritingTaskApi] 完成写作任务, taskId={}, articleId={}, finalAction={}", taskId, articleId, finalAction);
    }
}
