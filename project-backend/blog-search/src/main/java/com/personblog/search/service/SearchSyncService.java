package com.personblog.search.service;

public interface SearchSyncService{
    /**
     * 同步文章索引
     *
     * @param articleId 文章ID
     */
    void syncArticle(Long articleId);
    /**
     * 同步作者索引
     *
     * @param authorId 作者ID
     */
    void syncAuthor(Long authorId);
    /**
     * 同步专栏索引
     *
     * @param columnId 专栏ID
     */
    void syncColumn(Long columnId);
    /**
     * 同步所有文章索引
     */
    void syncAllArticles();
    /**
     * 同步所有作者索引
     */
    void syncAllAuthors();
    /**
     * 同步所有专栏索引
     */
    void syncAllColumns();
}
