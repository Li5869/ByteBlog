package com.personblog.article.controller.pythonCall;

import com.personblog.article.BizService.ArticlePublishBizService;
import com.personblog.article.dto.article.ArticlePublishDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.service.IArticleService;
import com.personblog.article.vo.Article.ArticlePublishVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文章操作内部API控制器
 * 供 Python AI 服务调用，用于文章相关操作
 *
 * @author LSH
 */
@Tag(name = "文章操作内部API", description = "供Python AI服务调用的内部接口")
@Slf4j
@RestController
@RequestMapping("/ai/article/internal")
@RequiredArgsConstructor
public class ArticleOperationController {
    private final ArticlePublishBizService articlePublishBizService;
    private final IArticleService articleService;

    @PostMapping("/create")
    @Operation(summary = "发布or草稿文章", description = "SmartAgent调用此接口发布文章")
    public JsonData<ArticlePublishVO> createOrDraft(
            @RequestParam("userId") Long userId,
            @RequestBody ArticlePublishDTO dto){
        ArticlePublishVO article = articlePublishBizService.createArticle(userId, dto);
        return JsonData.buildSuccess(article);
    }

    /**
     * 获取文章内容（供 Python AI 服务调用）
     * ES 索引不存正文（太大），需要走数据库查询
     */
    @GetMapping("/content/{articleId}")
    @Operation(summary = "获取文章内容", description = "根据文章ID获取文章的完整Markdown正文")
    public JsonData<String> getArticleContent(@PathVariable("articleId") Long articleId) {
        // 查询文章实体，仅获取 content 字段
        Article article = articleService.getById(articleId);
        if (article == null) {
            return JsonData.buildError("文章不存在");
        }
        return JsonData.buildSuccess(article.getContent());
    }
}
