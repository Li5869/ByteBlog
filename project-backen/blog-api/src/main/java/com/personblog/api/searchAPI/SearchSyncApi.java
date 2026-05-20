package com.personblog.api.searchAPI;

/**
 * 搜索同步 API 接口
 * 用于跨模块同步数据到搜索引擎（Elasticsearch）
 *
 * @author LSH
 */
public interface SearchSyncApi {

    /**
     * 同步文章到搜索引擎
     *
     * @param articleId 文章ID
     */
    void syncArticle(Long articleId);

    /**
     * 同步问题到搜索引擎
     *
     * @param questionId 问题ID
     */
    void syncQuestion(Long questionId);

    /**
     * 同步作者到搜索引擎
     *
     * @param authorId 作者ID
     */
    void syncAuthor(Long authorId);

    /**
     * 同步专栏到搜索引擎
     *
     * @param columnId 专栏ID
     */
    void syncColumn(Long columnId);
}
