package com.personblog.article.controller.Article;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.article.BizService.*;
import com.personblog.article.dto.article.ArticlePublishDTO;
import com.personblog.article.dto.article.ArticleQueryDTO;
import com.personblog.article.vo.Article.*;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.monitor.BusinessMetrics;
import com.personblog.common.result.JsonData;
import com.personblog.common.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
@Tag(name = "文章接口", description = "文章相关接口")
public class ArticleController {
    private final ArticleHotBizService articleHotBizService;
    private final BusinessMetrics businessMetrics;
    private final ArticleBannerBizService articleBannerBizService;
    private final ArticleListBizService articleListBizService;
    private final ArticleDetailBizService articleDetailBizService;
    private final ArticlePublishBizService articlePublishBizService;

    /**
     * 获取轮播图数据
     * 返回热门推荐文章列表，用于首页轮播图展示
     * @param size 返回数量，默认3，最大10
     * @return 轮播图列表
     */
    @Operation(summary = "获取轮播图数据", description = "获取首页轮播图数据，返回热门推荐文章列表")
    @GetMapping("/banners")
    public JsonData<List<BannerVO>> getBanners(
            @Parameter(description = "返回数量，默认3，最大10")
            @RequestParam(required = false) Integer size) {
        List<BannerVO> banners = articleBannerBizService.getBanners(size);
        return JsonData.buildSuccess(banners);
    }

    /**
     * 获取文章分页列表
     * 支持按分类、标签筛选，支持关键词搜索和排序
     * @return 分页文章列表
     */
    @Operation(summary = "获取文章列表", description = "获取文章分页列表，支持分类、标签筛选和排序")
    @PostMapping("/articles")
    public JsonData<Page<ArticleListVO>> getArticlePage(@RequestBody ArticleQueryDTO dto) {
        Page<ArticleListVO> page = articleListBizService.getArticlePage(dto);
        return JsonData.buildSuccess(page);
    }
    /**
     * 获取热门文章列表
     * 按浏览量排序，用于侧边栏展示
     * @param size 返回数量，默认5，最大20
     * @return 热门文章列表
     */
    @Operation(summary = "获取热门文章", description = "获取按浏览量排序的热门文章列表")
    @GetMapping("/articles/hot")
    public JsonData<List<HotArticleVO>> getHotArticles(
            @Parameter(description = "返回数量，默认5，最大20")
            @RequestParam(required = false) Integer size) {
        List<HotArticleVO> articles = articleHotBizService.getHotArticles(size);
        return JsonData.buildSuccess(articles);
    }

    @GetMapping("/articles/{id}")
    @Operation(summary = "获取文章基础信息", description = "获取文章基础信息（标题、摘要、正文、分类、标签等可缓存数据）")
    public JsonData<ArticleMetadataVO> getArticleMetadata(@PathVariable Long id) {
        ArticleMetadataVO vo = articleDetailBizService.getArticleMetadata(id);
        return JsonData.buildSuccess(vo);
    }
    /**
     * 获取随机文章列表
     * 用于首页"换一批"功能，返回随机排序的文章
     * @param size 返回数量，默认6，最大20
     * @return 随机文章列表
     */
    @Operation(summary = "获取随机文章", description = "获取随机排序的文章列表，用于'换一批'功能")
    @GetMapping("/articles/random")
    public JsonData<List<ArticleListVO>> getRandomArticles(
            @Parameter(description = "返回数量，默认6，最大20")
            @RequestParam(required = false) Integer size) {
        List<ArticleListVO> articles = articleListBizService.getRandomArticles(size);
        return JsonData.buildSuccess(articles);
    }
    @GetMapping("/articles/{id}/interaction")
    @Operation(summary = "获取文章互动数据", description = "获取实时互动数据（浏览量、点赞、收藏、评论数以及当前用户点赞/收藏状态），不缓存")
    public JsonData<ArticleInteractionVO> getArticleInteraction(@PathVariable Long id) {
        ArticleInteractionVO vo = articleDetailBizService.getArticleInteraction(id);
        return JsonData.buildSuccess(vo);
    }

    @GetMapping("/my-articles")
    @Operation(summary = "查询我的文章", description = "获取当前登录用户发布的文章列表，支持分页、状态筛选和排序")
    public JsonData<Page<MyArticleVO>> getMyArticles(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "文章状态筛选：0-草稿，1-已发布，2-已下架") @RequestParam(required = false) Integer status,
            @Parameter(description = "排序字段：created_at-按时间，likes-按点赞数，views-按阅读数") @RequestParam(required = false, defaultValue = "created_at") String orderBy) {
        Long userId = UserContextHolder.getUserId();
        Page<MyArticleVO> page = articlePublishBizService.getMyArticles(userId, current, size, status, orderBy);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取相关文章列表
     * 根据文章分类和标签获取相关文章推荐
     * @param id 当前文章ID
     * @param limit 返回数量，默认3，最大10
     * @return 相关文章列表
     */
    @Operation(summary = "获取相关文章", description = "根据文章分类和标签获取相关文章推荐")
    @GetMapping("/articles/{id}/related")
    public JsonData<List<RelatedArticleVO>> getRelatedArticles(
            @Parameter(description = "文章ID")
            @PathVariable Long id,
            @Parameter(description = "返回数量，默认3，最大10")
            @RequestParam(required = false) Integer limit) {
        List<RelatedArticleVO> articles = articleDetailBizService.getRelatedArticles(id, limit);
        return JsonData.buildSuccess(articles);
    }

    @Operation(summary = "创建文章", description = "创建文章并支持保存为草稿或直接发布")
    @PostMapping("/articles/publish")
    public JsonData<ArticlePublishVO> createArticle(@RequestBody ArticlePublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        ArticlePublishVO vo = articlePublishBizService.createArticle(userId, dto);
        businessMetrics.recordArticlePublish();
        return JsonData.buildSuccess(vo);
    }

    @Operation(summary = "更新文章", description = "更新文章并支持保存草稿或发布")
    @PutMapping("/articles/{id}")
    public JsonData<ArticlePublishVO> updateArticle(@PathVariable Long id, @RequestBody ArticlePublishDTO dto) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        if (Objects.equals(id, 0L)) {
            throw new BizException(BizCodeEnum.PARAMETER_ERROR);
        }
        ArticlePublishVO vo = articlePublishBizService.updateArticle(userId, id, dto);
        return JsonData.buildSuccess(vo);
    }

    @Operation(summary = "获取编辑文章回填数据", description = "仅作者可获取，用于编辑页面数据回填")
    @GetMapping("/articles/{id}/edit")
    public JsonData<ArticleEditVO> getEditArticle(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        ArticleEditVO vo = articlePublishBizService.getEditArticle(userId, id);
        return JsonData.buildSuccess(vo);
    }

    @Operation(summary = "删除文章", description = "删除当前登录用户创建的文章")
    @DeleteMapping("/articles/{id}")
    public JsonData<Void> deleteArticle(@PathVariable Long id) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }
        articlePublishBizService.deleteArticle(userId, id);
        return JsonData.buildSuccess();
    }
}
