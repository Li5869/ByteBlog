package com.personblog.api.interactionAPI;

public interface CollectedApi {
    //判断用户是否收藏文章
    boolean isCollected(Long articleId,Long userId);

}
