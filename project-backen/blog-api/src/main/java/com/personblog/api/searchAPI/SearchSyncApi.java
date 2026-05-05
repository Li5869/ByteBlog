// blog-api 模块
package com.personblog.api.searchAPI;

public interface SearchSyncApi {
    void syncArticle(Long articleId);
    void syncQuestion(Long questionId);
    void syncAuthor(Long authorId);
    void syncColumn(Long columnId);
}