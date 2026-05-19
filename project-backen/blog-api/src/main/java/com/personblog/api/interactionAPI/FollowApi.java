package com.personblog.api.interactionAPI;

import java.util.List;

public interface FollowApi {

    //获取用户关注的用户ID列表
    List<Long> getFollowingIds(Long userId);

    List<Long> checkBatchFollowStatus(List<Long> followingIds);
    /**
     * 获取用户的粉丝 ID 列表（即关注了该用户的人）
     *
     * @param userId 被关注者 ID
     * @return 粉丝 ID 列表
     */
    List<Long> getFollowerIds(Long userId);
}
