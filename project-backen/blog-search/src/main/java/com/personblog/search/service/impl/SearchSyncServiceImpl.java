package com.personblog.search.service.impl;

import com.personblog.api.searchAPI.*;
import com.personblog.common.dto.Search.ArticleSearchDTO;
import com.personblog.common.dto.Search.AuthorSearchDTO;
import com.personblog.common.dto.Search.ColumnSearchDTO;
import com.personblog.common.dto.Search.QuestionSearchDTO;
import com.personblog.search.entity.ArticleDocument;
import com.personblog.search.entity.AuthorDocument;
import com.personblog.search.entity.ColumnDocument;
import com.personblog.search.entity.QuestionDocument;
import com.personblog.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索数据同步服务实现
 * 负责将各模块数据同步到Elasticsearch索引
 * 使用ElasticsearchOperations替代Repository，避免ES不可达时Bean创建失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchSyncServiceImpl implements SearchSyncService, SearchSyncApi {

    private final ArticleSearchDataApi articleSearchDataApi;
    private final QuestionSearchDataApi questionSearchDataApi;
    private final AuthorSearchDataApi authorSearchDataApi;
    private final ColumnSearchDataApi columnSearchDataApi;
    private final ElasticsearchOperations elasticsearchOperations;

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
                elasticsearchOperations.save(convertToArticleDocument(dto));
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
    public void syncQuestion(Long questionId) {
        try {
            ensureIndexExists(QuestionDocument.class);
            QuestionSearchDTO dto = questionSearchDataApi.getQuestionForSearch(questionId);
            if (dto != null) {
                elasticsearchOperations.save(convertToQuestionDocument(dto));
                log.info("同步问题索引成功, questionId={}", questionId);
            } else {
                elasticsearchOperations.delete(String.valueOf(questionId), QuestionDocument.class);
                log.info("问题不存在或已删除，移除索引, questionId={}", questionId);
            }
        } catch (Exception e) {
            log.error("同步问题索引失败, questionId={}", questionId, e);
        }
    }

    @Override
    public void syncAuthor(Long authorId) {
        try {
            ensureIndexExists(AuthorDocument.class);
            AuthorSearchDTO dto = authorSearchDataApi.getAuthorForSearch(authorId);
            if (dto != null) {
                elasticsearchOperations.save(convertToAuthorDocument(dto));
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
                elasticsearchOperations.save(convertToColumnDocument(dto));
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
                    .map(this::convertToArticleDocument)
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
    public void syncAllQuestions() {
        log.info("========== 开始全量同步问题索引 ==========");
        long startTime = System.currentTimeMillis();
        try {
            ensureIndexExists(QuestionDocument.class);
            List<QuestionSearchDTO> questions = questionSearchDataApi.listAllQuestionsForSearch();
            List<QuestionDocument> documents = questions.stream()
                    .map(this::convertToQuestionDocument)
                    .toList();
            if (!documents.isEmpty()) {
                elasticsearchOperations.save(documents);
            }
            long cost = System.currentTimeMillis() - startTime;
            log.info("========== 全量同步问题索引完成，共{}条，耗时{}ms ==========", documents.size(), cost);
        } catch (Exception e) {
            log.error("全量同步问题索引失败", e);
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
                    .map(this::convertToAuthorDocument)
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
                    .map(this::convertToColumnDocument)
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
    //写入补全问题和文章
    private Completion buildTitleCompletion(String title, long weight) {
        List<String> inputs = new ArrayList<>();
        inputs.add(title);  // 完整标题
        // 如果标题含空格，拆分后各片段也加入（适合英文）
        if (title != null && title.contains(" ")) {
            String[] parts = title.split("\\s+");
            for (String part : parts) {
                if (part.length() >= 2) {
                    inputs.add(part);
                }
            }
        }
        Completion completion = new Completion(inputs.toArray(new String[0]));
        completion.setWeight((int) Math.min(weight, Integer.MAX_VALUE));
        return completion;
    }
    /**
     * 为博主昵称构建 Completion 对象
     */
    private Completion buildNicknameCompletion(String nickname) {
        Completion completion = new Completion(new String[]{nickname});
        completion.setWeight((int) Math.min((long) 1, Integer.MAX_VALUE));
        return completion;
    }
    // ==================== DTO → Document 转换 ====================

    private ArticleDocument convertToArticleDocument(ArticleSearchDTO dto) {
        ArticleDocument doc = new ArticleDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setSummary(dto.getSummary());
        doc.setCover(dto.getCover());
        doc.setAuthorId(dto.getAuthorId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setCategoryId(dto.getCategoryId());
        doc.setCategoryName(dto.getCategoryName());
        doc.setTags(dto.getTags());
        doc.setViews(dto.getViews());
        doc.setLikes(dto.getLikes());
        doc.setComments(dto.getComments());
        doc.setCollections(dto.getCollections());
        doc.setIsTop(dto.getIsTop());
        doc.setIsHot(dto.getIsHot());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        long weight = (dto.getViews() != null ? dto.getViews() : 0L)
                + (dto.getLikes() != null ? dto.getLikes() : 0L);
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(),weight));
        return doc;
    }

    private QuestionDocument convertToQuestionDocument(QuestionSearchDTO dto) {
        QuestionDocument doc = new QuestionDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setContent(dto.getContent());
        doc.setAuthorId(dto.getAuthorId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setTags(dto.getTags());
        doc.setViews(dto.getViews());
        doc.setAnswers(dto.getAnswers());
        doc.setLikes(dto.getLikes());
        doc.setIsSolved(dto.getIsSolved());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        long weight = (dto.getViews() != null ? dto.getViews() : 0L)
                + (dto.getLikes() != null ? dto.getLikes() : 0L);
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(), weight));
        return doc;
    }

    private AuthorDocument convertToAuthorDocument(AuthorSearchDTO dto) {
        AuthorDocument doc = new AuthorDocument();
        doc.setId(dto.getId());
        doc.setUsername(dto.getUsername());
        doc.setNickname(dto.getNickname());
        doc.setAvatar(dto.getAvatar());
        doc.setBio(dto.getBio());
        doc.setArticlesCount(dto.getArticlesCount());
        doc.setFansCount(dto.getFansCount());
        doc.setLikesCount(dto.getLikesCount());
        doc.setStatus(dto.getStatus());
        doc.setNicknameSuggest(buildNicknameCompletion(dto.getNickname()));
        return doc;
    }

    /**
     * DTO -> 专栏 Document
     */
    private ColumnDocument convertToColumnDocument(ColumnSearchDTO dto) {
        ColumnDocument doc = new ColumnDocument();
        doc.setId(dto.getId());
        doc.setTitle(dto.getTitle());
        doc.setDescription(dto.getDescription());
        doc.setCover(dto.getCover());
        doc.setUserId(dto.getUserId());
        doc.setAuthorName(dto.getAuthorName());
        doc.setAuthorAvatar(dto.getAuthorAvatar());
        doc.setArticlesCount(dto.getArticlesCount());
        doc.setSubscriptionCount(dto.getSubscriptionCount());
        doc.setViews(dto.getViews());
        doc.setStatus(dto.getStatus());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        doc.setTitleSuggest(buildTitleCompletion(dto.getTitle(), dto.getSubscriptionCount() != null ? dto.getSubscriptionCount() : 0));
        return doc;
    }
}
