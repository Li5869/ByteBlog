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
     * 发送点赞积分消息（给被点赞的作者发放积分）
     *
     * @param likerId    点赞者ID（用于防重复：同一人对同一内容只算一次）
     * @param authorId   被点赞内容的作者ID（积分接收者）
     * @param bizId      业务ID（文章ID、评论ID等）
     * @param targetType 互动目标类型（article / comment）
     */
    void sendLikePoint(Long likerId, Long authorId, Long bizId, String targetType);

    /**
     * 发送收藏积分消息
     *
     * @param operatorId      收藏者id
     * @param articleId   文章ID
     */
    void sendCollectionPoint(Long operatorId,Long authorId, Long articleId);

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
