package com.personblog.search.service.impl;

import com.personblog.api.searchAPI.ArticleSearchDataApi;
import com.personblog.api.searchAPI.AuthorSearchDataApi;
import com.personblog.api.searchAPI.ColumnSearchDataApi;
import com.personblog.api.searchAPI.SearchSyncApi;
import com.personblog.common.dto.Search.ArticleSearchDTO;
import com.personblog.common.dto.Search.AuthorSearchDTO;
import com.personblog.common.dto.Search.ColumnSearchDTO;
import com.personblog.search.convert.SearchConverter;
import com.personblog.search.entity.ArticleDocument;
import com.personblog.search.entity.AuthorDocument;
import com.personblog.search.entity.ColumnDocument;
import com.personblog.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchSyncServiceImpl implements SearchSyncService, SearchSyncApi {

    private final ArticleSearchDataApi articleSearchDataApi;
    private final AuthorSearchDataApi authorSearchDataApi;
    private final ColumnSearchDataApi columnSearchDataApi;
    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchConverter searchConverter;

    // ==================== 索引管理 ====================

    /**
     * 确保索引存在，不存在则创建并写入映射
     */
    private <T> void ensureIndexExists(Class<T> documentClass) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(documentClass);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("ES索引创建成功: {}", documentClass.getSimpleName());
        }
    }

    // ==================== 单条同步 ====================

    @Override
    public void syncArticle(Long articleId) {
        try {
            ensureIndexExists(ArticleDocument.class);
            ArticleSearchDTO dto = articleSearchDataApi.getArticleForSearch(articleId);
            if (dto != null) {
                elasticsearchOperations.save(searchConverter.convertToArticleDocument(dto));
                log.info("同步文章索引成功, articleId={}", articleId);
            } else {
                elasticsearchOperations.delete(String.valueOf(articleId), ArticleDocument.class);
                log.info("文章不存在或已下架，移除索引, articleId={}", articleId);
            }
        } catch (Exception e) {
            log.error("同步文章索引失败, articleId={}", articleId, e);
        }
    }

    @Override
    public void syncAuthor(Long authorId) {
        try {
            ensureIndexExists(AuthorDocument.class);
            AuthorSearchDTO dto = authorSearchDataApi.getAuthorForSearch(authorId);
            if (dto != null) {
                elasticsearchOperations.save(searchConverter.convertToAuthorDocument(dto));
                log.info("同步作者索引成功, authorId={}", authorId);
            } else {
                elasticsearchOperations.delete(String.valueOf(authorId), AuthorDocument.class);
                log.info("作者不存在或已封禁，移除索引, authorId={}", authorId);
            }
        } catch (Exception e) {
            log.error("同步作者索引失败, authorId={}", authorId, e);
        }
    }

    @Override
    public void syncColumn(Long columnId) {
        try {
            ensureIndexExists(ColumnDocument.class);
            ColumnSearchDTO dto = columnSearchDataApi.getColumnForSearch(columnId);
            if (dto != null) {
                elasticsearchOperations.save(searchConverter.convertToColumnDocument(dto));
                log.info("同步专栏索引成功, columnId={}", columnId);
            } else {
                elasticsearchOperations.delete(String.valueOf(columnId), ColumnDocument.class);
                log.info("专栏不存在或已删除，移除索引, columnId={}", columnId);
            }
        } catch (Exception e) {
            log.error("同步专栏索引失败, columnId={}", columnId, e);
        }
    }

    // ==================== 全量同步 ====================

    @Override
    public void syncAllArticles() {
        log.info("========== 开始全量同步文章索引 ==========");
        long startTime = System.currentTimeMillis();
        try {
            ensureIndexExists(ArticleDocument.class);
            List<ArticleSearchDTO> articles = articleSearchDataApi.listAllArticlesForSearch();
            List<ArticleDocument> documents = articles.stream()
                    .map(searchConverter::convertToArticleDocument)
                    .toList();
            if (!documents.isEmpty()) {
                elasticsearchOperations.save(documents);
            }
            long cost = System.currentTimeMillis() - startTime;
            log.info("========== 全量同步文章索引完成，共{}条，耗时{}ms ==========", documents.size(), cost);
        } catch (Exception e) {
            log.error("全量同步文章索引失败", e);
        }
    }

    @Override
    public void syncAllAuthors() {
        log.info("========== 开始全量同步作者索引 ==========");
        long startTime = System.currentTimeMillis();
        try {
            ensureIndexExists(AuthorDocument.class);
            List<AuthorSearchDTO> authors = authorSearchDataApi.listAllAuthorsForSearch();
            List<AuthorDocument> documents = authors.stream()
                    .map(searchConverter::convertToAuthorDocument)
                    .toList();
            if (!documents.isEmpty()) {
                elasticsearchOperations.save(documents);
            }
            long cost = System.currentTimeMillis() - startTime;
            log.info("========== 全量同步作者索引完成，共{}条，耗时{}ms ==========", documents.size(), cost);
        } catch (Exception e) {
            log.error("全量同步作者索引失败", e);
        }
    }

    @Override
    public void syncAllColumns() {
        log.info("========== 开始全量同步专栏索引 ==========");
        long startTime = System.currentTimeMillis();
        try {
            ensureIndexExists(ColumnDocument.class);
            List<ColumnSearchDTO> columns = columnSearchDataApi.listAllColumnsForSearch();
            List<ColumnDocument> documents = columns.stream()
                    .map(searchConverter::convertToColumnDocument)
                    .toList();
            if (!documents.isEmpty()) {
                elasticsearchOperations.save(documents);
            }
            long cost = System.currentTimeMillis() - startTime;
            log.info("========== 全量同步专栏索引完成，共{}条，耗时{}ms ==========", documents.size(), cost);
        } catch (Exception e) {
            log.error("全量同步专栏索引失败", e);
        }
    }
}
