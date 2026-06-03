package com.personblog.api.pointAPI;

/**
 * 积分消息发送 API 接口
 * 用于跨模块异步发送积分变动消息（MQ）
 * 调用方只需依赖 blog-api，无需关心 MQ 队列细节
 *
 * @author LSH
 * @since 2026-06-03
 */
public interface PointMqApi {

    /**
     * 发送文章发布积分消息
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     */
    void sendArticlePoint(Long userId, Long articleId);

    /**
     * 发送点赞积分消息
     *
     * @param userId   用户ID
     * @param bizId    业务ID（文章ID、评论ID等）
     * @param bizType  业务类型（article_liked / comment_liked / answer_liked）
     */
    void sendLikePoint(Long userId, Long bizId, String bizType);

    /**
     * 发送收藏积分消息
     *
     * @param userId      用户ID
     * @param articleId   文章ID
     */
    void sendCollectionPoint(Long userId, Long articleId);

    /**
     * 发送管理员调整积分消息
     *
     * @param userId      目标用户ID
     * @param points      积分变动值（正数增加，负数减少）
     * @param description 调整原因描述
     * @param operatorId  操作管理员ID
     */
    void sendAdminAdjustPoint(Long userId, Integer points, String description, Long operatorId);
}
