package com.personblog.common.api;

import java.util.List;

/**
 * 粉丝查询接口
 * 由 blog-interaction 模块实现，用于 WebSocket 模块获取用户粉丝列表
 *
 * @author LSH
 */
public interface FollowerApi {

    /**
     * 获取用户的粉丝 ID 列表（即关注了该用户的人）
     *
     * @param userId 被关注者 ID
     * @return 粉丝 ID 列表
     */
    List<Long> getFollowerIds(Long userId);
}
