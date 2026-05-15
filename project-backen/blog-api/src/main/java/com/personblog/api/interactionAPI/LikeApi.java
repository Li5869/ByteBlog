package com.personblog.api.interactionAPI;

import java.util.List;
import java.util.Map;
import java.util.Set;

// blog-api/src/.../api/interaction/LikeApi.java
public interface LikeApi {
    
    /** 判断用户是否点赞了某目标 */
    boolean isLiked(Long targetId, Long userId,String targetType);
    
    /** 获取某文章的点赞数 */
    long getLikeCount(Long targetId,String targetType);
    //批量获取是否点赞，返回值为点过赞的评论
    Set<Long> batchIsLike(List<Long> targetIds,String targetType);
    //批量获取点赞总数
    Map<Long,Long> getLikesTime(List<Long> targetIds,String targetType);
    //同步点赞数据到数据库
    void readLikesTimesAnd2DB(String TargetType,int maxSize);
}
