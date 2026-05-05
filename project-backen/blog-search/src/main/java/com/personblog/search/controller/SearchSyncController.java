package com.personblog.search.controller;

import com.personblog.common.result.JsonData;
import com.personblog.search.service.SearchSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索索引同步管理控制器
 * 
 * 职责：管理 Elasticsearch 索引的同步操作
 * 包括：文章、问题、作者、专栏 的单条同步和全量同步
 *
 * @author LSH
 */
@Tag(name = "搜索索引管理", description = "ES索引同步相关接口（管理侧）")
@RestController
@RequestMapping("/search/sync")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchSyncController {

    private final SearchSyncService searchSyncService;

    // ==================== 文章索引同步 ====================

    @Operation(summary = "同步文章索引", description = "将指定文章同步到ES索引")
    @PostMapping("/article/{articleId}")
    public JsonData<Void> syncArticle(@PathVariable Long articleId) {
        searchSyncService.syncArticle(articleId);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "全量同步文章", description = "将所有文章同步到ES索引")
    @PostMapping("/article/all")
    public JsonData<Void> syncAllArticles() {
        searchSyncService.syncAllArticles();
        return JsonData.buildSuccess();
    }

    // ==================== 问题索引同步 ====================

    @Operation(summary = "同步问题索引", description = "将指定问题同步到ES索引")
    @PostMapping("/question/{questionId}")
    public JsonData<Void> syncQuestion(@PathVariable Long questionId) {
        searchSyncService.syncQuestion(questionId);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "全量同步问题", description = "将所有问题同步到ES索引")
    @PostMapping("/question/all")
    public JsonData<Void> syncAllQuestions() {
        searchSyncService.syncAllQuestions();
        return JsonData.buildSuccess();
    }

    // ==================== 作者索引同步 ====================

    @Operation(summary = "同步作者索引", description = "将指定作者同步到ES索引")
    @PostMapping("/author/{authorId}")
    public JsonData<Void> syncAuthor(@PathVariable Long authorId) {
        searchSyncService.syncAuthor(authorId);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "全量同步作者", description = "将所有作者同步到ES索引")
    @PostMapping("/author/all")
    public JsonData<Void> syncAllAuthors() {
        searchSyncService.syncAllAuthors();
        return JsonData.buildSuccess();
    }

    // ==================== 专栏索引同步 ====================

    @Operation(summary = "同步专栏索引", description = "将指定专栏同步到ES索引")
    @PostMapping("/column/{columnId}")
    public JsonData<Void> syncColumn(@PathVariable Long columnId) {
        searchSyncService.syncColumn(columnId);
        return JsonData.buildSuccess();
    }

    @Operation(summary = "全量同步专栏", description = "将所有专栏同步到ES索引")
    @PostMapping("/column/all")
    public JsonData<Void> syncAllColumns() {
        searchSyncService.syncAllColumns();
        return JsonData.buildSuccess();
    }
}
