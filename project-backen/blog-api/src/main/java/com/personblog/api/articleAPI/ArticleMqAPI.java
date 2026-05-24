package com.personblog.api.articleAPI;

import com.personblog.common.dto.MqMessage.Interaction.BrowseHistoryMessage;
import com.personblog.common.dto.MqMessage.Interaction.CollectionMessage;
import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;

import java.util.List;

public interface ArticleMqAPI {
    /**
     * 更新文章点赞数
     * @param dtoList 点赞消息DTO列表
     */
    void updateLikeCount(List<LikeMessage> dtoList);

    /**
     * 更新文章收藏数
     * @param dto 收藏消息DTO
     */
    void updateCollectionCount(CollectionMessage dto);

    /**
     * 更新文章浏览数
     * @param dtoList 浏览历史消息DTO列表
     */
    void updateBrowseCount(List<BrowseHistoryMessage> dtoList);

    /**
     * 更新文章评论数
     * @param articleId 文章ID
     * @param dealt 评论数
     */
    void updateCommentCount(Long articleId,int dealt);
}
