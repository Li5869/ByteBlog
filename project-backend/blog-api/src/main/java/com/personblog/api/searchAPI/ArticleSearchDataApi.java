package com.personblog.api.searchAPI;

import com.personblog.common.dto.Search.ArticleSearchDTO;

import java.util.List;

/**
 * 文章搜索数据API —— 供blog-search模块从blog-article拉取文章数据
 */
public interface ArticleSearchDataApi {

    /**
     * 获取所有已发布文章的搜索数据
     */
    List<ArticleSearchDTO> listAllArticlesForSearch();

    /**
     * 获取单个文章的搜索数据
     */
    ArticleSearchDTO getArticleForSearch(Long articleId);
}
