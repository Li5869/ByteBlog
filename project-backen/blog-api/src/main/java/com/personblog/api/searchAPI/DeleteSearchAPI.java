package com.personblog.api.searchAPI;

public interface DeleteSearchAPI {
    void deleteArticle(Long articleId);
    void deleteQuestion(Long questionId);
    void deleteAuthor(Long authorId);
    void deleteColumn(Long columnId);
}
