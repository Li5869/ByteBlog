package com.personblog.search.init;

import com.personblog.search.service.SearchSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 数据初始化器
 *
 * 应用启动后执行全量数据同步到ES
 * 通过 search.sync-on-startup 配置控制是否启用
 * 通过 search.enabled 配置控制整个搜索模块是否启用
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "search.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ElasticsearchDataInitializer {

    private final SearchSyncService searchSyncService;

    /**
     * 应用启动后执行全量数据同步
     *
     * 执行顺序：文章 → 问题 → 作者 → 专栏
     * 原因：文章数据量通常最大，优先同步以尽快提供搜索服务
     */
    @PostConstruct
    public void init() {
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   Elasticsearch 数据初始化 - 开始全量同步       ║");
        log.info("╚══════════════════════════════════════════════════╝");

        long totalStart = System.currentTimeMillis();

        // 1. 同步文章索引
        syncArticles();

        // 2. 同步问题索引
        syncQuestions();

        // 3. 同步作者索引
        syncAuthors();

        // 4. 同步专栏索引
        syncColumns();

        long totalCost = System.currentTimeMillis() - totalStart;
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║   Elasticsearch 数据初始化 - 全量同步完成       ║");
        log.info("║   总耗时: {}ms                                 ║", totalCost);
        log.info("╚══════════════════════════════════════════════════╝");
    }

    private void syncArticles() {
        try {
            log.info(">>> [1/4] 开始同步文章索引...");
            searchSyncService.syncAllArticles();
            log.info(">>> [1/4] 文章索引同步完成 ✓");
        } catch (Exception e) {
            log.error(">>> [1/4] 文章索引同步失败 ✗，将在后续增量同步中修复", e);
        }
    }

    private void syncQuestions() {
        try {
            log.info(">>> [2/4] 开始同步问题索引...");
            searchSyncService.syncAllQuestions();
            log.info(">>> [2/4] 问题索引同步完成 ✓");
        } catch (Exception e) {
            log.error(">>> [2/4] 问题索引同步失败 ✗，将在后续增量同步中修复", e);
        }
    }

    private void syncAuthors() {
        try {
            log.info(">>> [3/4] 开始同步作者索引...");
            searchSyncService.syncAllAuthors();
            log.info(">>> [3/4] 作者索引同步完成 ✓");
        } catch (Exception e) {
            log.error(">>> [3/4] 作者索引同步失败 ✗，将在后续增量同步中修复", e);
        }
    }

    private void syncColumns() {
        try {
            log.info(">>> [4/4] 开始同步专栏索引...");
            searchSyncService.syncAllColumns();
            log.info(">>> [4/4] 专栏索引同步完成 ✓");
        } catch (Exception e) {
            log.error(">>> [4/4] 专栏索引同步失败 ✗，将在后续增量同步中修复", e);
        }
    }
}
