package com.personblog.api.interactionAPI;

import java.util.List;

public interface FollowApi {
    //判断是否是粉丝
    boolean isFollowed(Long followedId,Long followingId);

    //获取用户关注的用户ID列表
    List<Long> getFollowingIds(Long userId);

    List<Long> checkBatchFollowStatus(List<Long> followingIds);
}
