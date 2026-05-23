package com.personblog.api.usrAPI;

import com.personblog.common.dto.MqMessage.user.UserLikeMessageDTO;
import com.personblog.common.dto.User.UserDTO;

import java.util.Collection;
import java.util.List;

/**
 * 用户 API 接口
 * 用于跨模块调用用户服务，获取用户信息和更新用户统计数据
 *
 * @author LSH
 */
public interface UseApi {

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID集合（空集合表示获取所有用户）
     * @return 用户DTO列表
     */
    List<UserDTO> getUserInfo(Collection<Long> userIds);

    /**
     * 更新用户粉丝数
     *
     * @param userId 用户ID
     * @param delta  变化量（正数增加，负数减少）
     */
    void updateFanCount(Long userId, int delta);

    /**
     * 更新用户关注数
     *
     * @param userId 用户ID
     * @param delta  变化量（正数增加，负数减少）
     */
    void updateFollowingCount(Long userId, int delta);

    /**
     * 批量更新用户获赞数
     * 由 MQ 消费者调用，当文章/评论被点赞时更新作者获赞数
     *
     * @param dtoList 用户获赞消息列表
     */
    void batchUpdateLikesCount(List<UserLikeMessageDTO> dtoList);

    /**
     * 更新用户收藏数
     *
     * @param userId 用户ID
     * @param delta  变化量（正数增加，负数减少）
     */
    void updateCollectionsCount(Long userId, int delta);

    /**
     * 更新用户文章数
     *
     * @param userId 用户ID
     * @param delta  变化量（正数增加，负数减少）
     */
    void updateArticlesCount(Long userId, int delta);
}
