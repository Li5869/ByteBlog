package com.personblog.ai.controller;

import com.personblog.ai.BizService.ArticlePolishService;
import com.personblog.ai.BizService.ArticleSummaryService;
import com.personblog.ai.BizService.ArticleTitleService;
import com.personblog.ai.dto.ArticlePolishDTO;
import com.personblog.ai.dto.ArticleSummaryDTO;
import com.personblog.ai.dto.ArticleTitleDTO;
import com.personblog.ai.vo.ArticlePolishVO;
import com.personblog.ai.vo.ArticleSummaryVO;
import com.personblog.ai.vo.ArticleTitleVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章 AI 接口
 *
 * @author LSH
 */
@Tag(name = "文章AI接口", description = "文章相关的AI功能")
@RestController
@RequestMapping("/ai/article")
@RequiredArgsConstructor
public class ArticleAIController {

    private final ArticleSummaryService articleSummaryService;
    private final ArticleTitleService articleTitleService;
    private final ArticlePolishService articlePolishService;

    @Operation(summary = "生成文章摘要")
    @PostMapping("/summary")
    public JsonData<ArticleSummaryVO> generateSummary(@Valid @RequestBody ArticleSummaryDTO dto) {
        return JsonData.buildSuccess(articleSummaryService.generateSummary(dto));
    }

    @Operation(summary = "生成文章标题")
    @PostMapping("/title")
    public JsonData<ArticleTitleVO> generateTitle(@Valid @RequestBody ArticleTitleDTO dto){
        return JsonData.buildSuccess(articleTitleService.generateTitle(dto));
    }

    @Operation(summary = "文章内容润色")
    @PostMapping("/polish")
    public JsonData<ArticlePolishVO> polishArticle(@Valid @RequestBody ArticlePolishDTO dto){
        return JsonData.buildSuccess(articlePolishService.polishArticle(dto));
    }
}