package com.personblog.api.writingAPI;

/**
 * 写作任务 API 接口
 * 用于跨模块更新写作任务状态
 *
 * @author LSH
 */
public interface WritingTaskApi {

    /**
     * 完成写作任务（更新文章ID和最终动作）
     *
     * @param taskId      写作任务ID
     * @param articleId   文章ID
     * @param finalAction 最终动作（publish-发布，draft-存草稿）
     */
    void completeTask(Long taskId, Long articleId, String finalAction);
}
