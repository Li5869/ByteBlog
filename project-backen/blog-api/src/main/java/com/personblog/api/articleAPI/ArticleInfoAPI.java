package com.personblog.api.articleAPI;

import com.personblog.common.dto.Interaction.BrowseHistoryMessageDTO;
import com.personblog.common.dto.Interaction.CollectionMessageDTO;
import com.personblog.common.dto.Interaction.LikeMessageDTO;

import java.util.List;

public interface ArticleInfoAPI {
    /**
     * 更新文章点赞数
     * @param dtoList 点赞消息DTO列表
     */
    void updateLikeCount(List<LikeMessageDTO> dtoList);
    /**
     * 更新文章收藏数
     * @param dto 收藏消息DTO
     */
    void updateCollectionCount(CollectionMessageDTO dto);
    /**
     * 更新文章浏览数
     * @param dtoList 浏览历史消息DTO列表
     */
    void updateBrowseCount(List<BrowseHistoryMessageDTO> dtoList);
    /**
     * 更新文章评论数
     * @param articleId 文章ID
     * @param dealt 评论数
     */
    void updateCommentCount(Long articleId,int dealt);
    /**
     * 获取文章作者ID
     * @param articleId 文章ID
     * @return 作者ID
     */
    Long getArticleAuthorId(Long articleId);

    /**
     * 更新文章审核状态
     * @param articleId 文章ID
     * @param status 审核状态
     */
    void updateArticleReviewStatus(Long articleId,String status);

    /**
     * 获取文章标题
     * @param articleId 文章ID
     * @return 文章标题
     */
    String getArticleTitle(Long articleId);

    /**
     * 刷新热门文章标记
     * 根据综合热度分（浏览量+点赞+收藏+评论）计算 Top N 并更新 is_hot 字段
     */
    void refreshHotArticles();
}
