package com.personblog.api.interactionAPI;

/**
 * 浏览历史 API 接口
 * 用于跨模块调用浏览历史服务，记录用户浏览行为
 *
 * @author LSH
 */
public interface BrowseHistoryApi {

    /**
     * 记录用户浏览文章行为
     * 将浏览记录写入 Redis，由定时任务同步到数据库
     *
     * @param userId    用户ID（可为 null，表示未登录用户）
     * @param articleId 文章ID
     */
    void recordBrowse(Long userId, Long articleId);

    /**
     * 同步浏览历史到数据库
     * 将 Redis 中的浏览记录持久化到 MySQL，并更新文章浏览量
     */
    void syncBrowseHistory2DB();
}
