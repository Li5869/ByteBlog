package com.personblog.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.personblog.search.dto.SuggestResultDTO;
import com.personblog.search.service.SearchSuggestService;
import com.personblog.search.vo.SuggestItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 搜索建议服务 - 基于 ES Completion Suggester
 * <p>
 * 依赖 elasticsearch-java 9.2.6
 * <p>
 * Completion Suggester 使用内存 FST 实现毫秒级前缀匹配，
 * 不返回 _source，仅返回建议文本和文档 _id。
 * <p>
 * 注意：SDE 6.0.4 的 ElasticsearchOperations 不包含 suggest() API，
 * 因此直接使用 elasticsearch-java 原生客户端执行建议查询。
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchSuggestServiceImpl implements SearchSuggestService {

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public SuggestResultDTO suggest(String keyword, int size) {
        SuggestResultDTO result = new SuggestResultDTO();
        if (StrUtil.isBlank(keyword)) {
            return result;
        }
        int perType = Math.max(1, size / 4);
        result.setArticles(doSuggest("article", "titleSuggest", keyword, perType, "article"));
        result.setQuestions(doSuggest("question", "titleSuggest", keyword, perType, "question"));
        result.setAuthors(doSuggest("author", "nicknameSuggest", keyword, perType, "author"));
        result.setColumns(doSuggest("column", "titleSuggest", keyword, perType, "column"));
        return result;
    }

    /**
     * 执行 Completion Suggester 查询
     * <p>
     * API 调用层级（elasticsearch-java 9.2.6）：
     * <pre>
     * Suggester                          // 顶层建议器容器
     *   .text(keyword)                   // 全局搜索文本（可省略）
     *   .suggesters(name, FieldSuggester) // 命名建议器，name 任意字符串
     * FieldSuggester                     // 字段建议器（标签联合体）
     *   .prefix(keyword)                 // ← prefix 在 FieldSuggester 上！
     *   .completion(CompletionSuggester)  // 补全建议器变体
     * CompletionSuggester                // 补全配置
     *   .field("xxxSuggest")            // 指定 completion 字段
     *   .size(n)                         // 返回条数
     *   .skipDuplicates(true)            // 去重
     * </pre>
     */
    private List<SuggestItemVO> doSuggest(String index, String field,
                                           String keyword, int size, String type) {
        try {
            Suggester suggester = Suggester.of(s -> s
                    .text(keyword)
                    .suggesters("s", FieldSuggester.of(fs -> fs
                            .prefix(keyword)
                            .completion(cs -> cs
                                    .field(field)
                                    .size(size)
                                    .skipDuplicates(true)
                            )
                    ))
            );

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                            .index(index)
                            .suggest(suggester),
                    Void.class
            );

            return extractSuggestions(response, type);
        } catch (IOException e) {
            log.error("获取{}建议失败: keyword={}", index, keyword, e);
            return Collections.emptyList();
        }
    }

    /**
     * 从 SearchResponse 中提取 Completion 建议
     * <p>
     * CompletionSuggestOption 可用方法：
     * - text()       → 建议文本（String）
     * - id()         → 文档 _id（String）
     * - index()      → 索引名
     * - score()      → 匹配得分
     */
    private List<SuggestItemVO> extractSuggestions(SearchResponse<?> response, String type) {
        List<SuggestItemVO> result = new ArrayList<>();
        if (response.suggest() == null) {
            return result;
        }
        var suggestions = response.suggest().get("s");
        if (suggestions == null || suggestions.isEmpty()) {
            return result;
        }
        var options = suggestions.getFirst().completion().options();
        for (CompletionSuggestOption<?> option : options) {
            SuggestItemVO vo = new SuggestItemVO();
            vo.setTitle(option.text());
            vo.setType(type);
            if (option.id() != null) {
                try {
                    vo.setId(Long.parseLong(option.id()));
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(vo);
        }
        return result;
    }
}
