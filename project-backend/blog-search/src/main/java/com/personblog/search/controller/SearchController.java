package com.personblog.search.controller;

import cn.hutool.core.util.StrUtil;
import com.personblog.common.result.JsonData;
import com.personblog.search.dto.SearchQueryDTO;
import com.personblog.search.dto.SearchResultDTO;
import com.personblog.search.dto.SuggestResultDTO;
import com.personblog.search.service.SearchService;
import com.personblog.search.service.SearchSuggestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索查询控制器
 * 
 * 职责：提供全局搜索和搜索建议功能
 * 包括：文章、作者、专栏 的全文检索和补全建议
 *
 * @author LSH
 */
@Tag(name = "搜索服务", description = "全文检索相关接口（用户侧）")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class SearchController {

    private final SearchService searchService;
    private final SearchSuggestService searchSuggestService;

    @Operation(summary = "全局搜索", description = "搜索文章、作者、专栏，通过type参数指定类型：article/author/column/all")
    @GetMapping
    public JsonData<SearchResultDTO> search(SearchQueryDTO queryDTO) {
        SearchResultDTO result = searchService.search(queryDTO);
        return JsonData.buildSuccess(result);
    }

    @Operation(summary = "搜索建议", description = "基于 ES Completion Suggester 的实时前缀补全")
    @GetMapping("/suggest")
    public JsonData<SuggestResultDTO> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "12") int size) {
        if (StrUtil.isBlank(keyword)) {
            return JsonData.buildSuccess(new SuggestResultDTO());
        }
        SuggestResultDTO result = searchSuggestService.suggest(keyword.trim(), size);
        return JsonData.buildSuccess(result);
    }
}
