package com.personblog.api.AIAPI;

/**
 * AI 文章草稿 API 接口
 * 用于跨模块调用 AI 写作草稿服务
 *
 * @author LSH
 */
public interface AiArticleDraftApi {

    /**
     * 根据写作任务ID删除草稿
     * 当用户发布文章或取消写作任务时调用
     *
     * @param taskId 写作任务ID
     */
    void deleteByTaskId(Long taskId);
}
