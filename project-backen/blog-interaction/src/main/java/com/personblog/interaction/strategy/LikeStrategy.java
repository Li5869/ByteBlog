package com.personblog.interaction.strategy;

/**
 * 点赞策略接口
 * 用于处理不同类型内容的点赞操作
 *
 * @author LSH
 */
public interface LikeStrategy {

    /**
     * 保存点赞记录
     * @param userId 用户ID
     * @param targetId 目标ID
     */
    void saveLike(Long userId, Long targetId);

    /**
     * 删除点赞记录
     * @param userId 用户ID
     * @param targetId 目标ID
     */
    void removeLike(Long userId, Long targetId);

    /**
     * 判断某个业务是否点赞
     * @param userId 用户id
     * @param targetId 业务id
     */
    Boolean getIsLike(Long userId,Long targetId);

    /**
     * 全量同步点赞相关到缓存
     */
    void AllSync2Cache();
}
