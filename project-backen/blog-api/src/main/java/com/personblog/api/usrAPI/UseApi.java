package com.personblog.api.usrAPI;

import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.dto.User.UserLikeMessageDTO;

import java.util.Collection;
import java.util.List;

public interface UseApi {
    // 获取用户信息
    List<UserDTO> getUserInfo(Collection<Long> userIds);
    // 更新用户粉丝数
    void updateFanCount(Long userId,int delta);
    // 更新用户关注数
    void updateFollowingCount(Long userId,int delta);
    // 批量更新用户点赞数
    void batchUpdateLikesCount(List<UserLikeMessageDTO> dtoList);
    // 更新用户收藏数
    void updateCollectionsCount(Long userId, int delta);
    //更新用户文章数
    void updateArticlesCount(Long userId, int delta);
}
