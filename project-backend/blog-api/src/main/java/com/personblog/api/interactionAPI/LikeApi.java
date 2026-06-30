package com.personblog.api.interactionAPI;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 点赞 API 接口
 * 用于跨模块调用点赞服务，支持文章、评论、问题、回答等多种目标类型
 *
 * @author LSH
 */
public interface LikeApi {

    /**
     * 判断用户是否点赞了某目标
     *
     * @param targetId   目标ID（文章ID、评论ID等）
     * @param userId     用户ID
     * @param targetType 目标类型（article/comment/question/answer）
     * @return true 表示已点赞，false 表示未点赞
     */
    boolean isLiked(Long targetId, Long userId, String targetType);

    /**
     * 获取某目标的点赞数
     *
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return 点赞数
     */
    long getLikeCount(Long targetId, String targetType);

    /**
     * 批量获取用户是否点赞了多个目标
     *
     * @param targetIds  目标ID列表
     * @param targetType 目标类型
     * @return 已点赞的目标ID集合
     */
    Set<Long> batchIsLike(List<Long> targetIds, String targetType);

    /**
     * 批量获取多个目标的点赞数
     *
     * @param targetIds  目标ID列表
     * @param targetType 目标类型
     * @return 目标ID -> 点赞数的映射
     */
    Map<Long, Long> getLikesTime(List<Long> targetIds, String targetType);

    /**
     * 同步点赞数据到数据库
     * 将 Redis 中的点赞数据持久化到 MySQL
     *
     * @param targetType 目标类型
     * @param maxSize    最大处理数量
     */
    void readLikesTimesAnd2DB(String targetType, int maxSize);
}
