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
}
