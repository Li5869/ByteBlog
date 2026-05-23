package com.personblog.api.searchAPI;

/**
 * 搜索删除 API 接口
 * 用于跨模块删除搜索引擎中的索引数据
 *
 * @author LSH
 */
public interface DeleteSearchAPI {

    /**
     * 删除文章索引
     *
     * @param articleId 文章ID
     */
    void deleteArticle(Long articleId);

    /**
     * 删除问题索引
     *
     * @param questionId 问题ID
     */
    void deleteQuestion(Long questionId);

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
