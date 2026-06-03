package com.personblog.search.service.impl;

import com.personblog.api.searchAPI.DeleteSearchAPI;
import com.personblog.search.entity.ArticleDocument;
import com.personblog.search.entity.AuthorDocument;
import com.personblog.search.entity.ColumnDocument;
import com.personblog.search.service.DeleteSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * 搜索索引删除服务
 * 使用ElasticsearchOperations替代Repository，避免ES不可达时Bean创建失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class DeleteSearchImpl implements DeleteSearchService, DeleteSearchAPI {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void deleteArticle(Long articleId) {
        try {
            elasticsearchOperations.delete(String.valueOf(articleId), ArticleDocument.class);
            log.info("删除文章索引成功, articleId={}", articleId);
        } catch (Exception e) {
            log.error("删除文章索引失败, articleId={}", articleId, e);
        }
    }

    @Override
    public void deleteAuthor(Long authorId) {
        try {
            elasticsearchOperations.delete(String.valueOf(authorId), AuthorDocument.class);
            log.info("删除作者索引成功, authorId={}", authorId);
        } catch (Exception e) {
            log.error("删除作者索引失败, authorId={}", authorId, e);
        }
    }

    @Override
    public void deleteColumn(Long columnId) {
        try {
            elasticsearchOperations.delete(String.valueOf(columnId), ColumnDocument.class);
            log.info("删除专栏索引成功, columnId={}", columnId);
        } catch (Exception e) {
            log.error("删除专栏索引失败, columnId={}", columnId, e);
        }
    }
}
