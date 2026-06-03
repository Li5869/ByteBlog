package com.personblog.search.service;

public interface DeleteSearchService {
    /**
     * 删除文章索引
     *
     * @param articleId 文章ID
     */
    void deleteArticle(Long articleId);

    /**
     * 删除作者索引
     *
     * @param authorId 作者ID
     */
    void deleteAuthor(Long authorId);

    /**
     * 删除专栏索引
     *
     * @param columnId 专栏ID
     */
    void deleteColumn(Long columnId);
}
