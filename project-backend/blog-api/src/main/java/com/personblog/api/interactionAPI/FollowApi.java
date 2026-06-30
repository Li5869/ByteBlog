package com.personblog.api.interactionAPI;

import java.util.List;

/**
 * 关注 API 接口
 * 用于跨模块调用关注服务，处理用户关注关系
 *
 * @author LSH
 */
public interface FollowApi {

    /**
     * 获取用户关注的用户ID列表
     *
     * @param userId 用户ID
     * @return 该用户关注的所有用户ID列表
     */
    List<Long> getFollowingIds(Long userId);

    /**
     * 批量检查当前用户是否关注了指定用户
     *
     * @param followingIds 待检查的用户ID列表
     * @return 当前用户已关注的用户ID列表
     */
    List<Long> checkBatchFollowStatus(List<Long> followingIds);

    /**
     * 获取用户的粉丝ID列表（即关注了该用户的人）
     *
     * @param userId 被关注者ID
     * @return 粉丝ID列表
     */
    List<Long> getFollowerIds(Long userId);
}
