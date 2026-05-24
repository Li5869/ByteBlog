package com.personblog.api.articleAPI;

public interface ArticleAPI {
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
}
