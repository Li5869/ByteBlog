package com.personblog.article.controller.Admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.admin.aspect.RecordLog;
import com.personblog.article.BizService.ArticleAdminBizService;
import com.personblog.article.dto.article.AdminArticleQueryDTO;
import com.personblog.article.vo.AdminArticleVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端文章管理控制器
 * 支持文章的列表查询、详情查看、审核、置顶和删除
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/articles")
@RequiredArgsConstructor
@Tag(name = "管理端-文章管理", description = "管理后台的文章管理接口")
public class AdminArticleController {

    private final ArticleAdminBizService articleAdminBizService;

    /**
     * 获取文章列表（分页）
     * 支持关键词搜索、状态筛选、审核状态筛选、分类筛选和排序
     */
    @Operation(summary = "获取文章列表", description = "分页查询文章列表，支持多种筛选条件")
    @PostMapping("/list")
    public JsonData<Page<AdminArticleVO>> getArticlePage(@RequestBody AdminArticleQueryDTO dto) {
        Page<AdminArticleVO> page = articleAdminBizService.getAdminArticlePage(dto);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情", description = "获取单篇文章的完整详情")
    @GetMapping("/{id}")
    public JsonData<AdminArticleVO> getArticleDetail(
            @Parameter(description = "文章ID") @PathVariable Long id) {
        AdminArticleVO article = articleAdminBizService.getAdminArticleDetail(id);
        return JsonData.buildSuccess(article);
    }

    /**
     * 删除文章（逻辑删除）
     */
    @RecordLog(Type = "delete", businessType = "article", description = "删除文章")
    @Operation(summary = "删除文章", description = "管理员删除指定文章")
    @DeleteMapping("/{id}")
    public JsonData<Void> deleteArticle(
            @Parameter(description = "文章ID") @PathVariable Long id) {
        articleAdminBizService.deleteArticleByAdmin(id);
        return JsonData.buildSuccess();
    }

    /**
     * 审核通过
     */
    @RecordLog(Type = "review", businessType = "article", description = "审核通过文章")
    @Operation(summary = "审核通过", description = "管理员审核通过文章")
    @PutMapping("/{id}/approve")
    public JsonData<Void> approveArticle(
            @Parameter(description = "文章ID") @PathVariable Long id) {
        articleAdminBizService.approveArticle(id);
        return JsonData.buildSuccess();
    }

    /**
     * 审核拒绝
     */
    @RecordLog(Type = "review", businessType = "article", description = "审核拒绝文章")
    @Operation(summary = "审核拒绝", description = "管理员审核拒绝文章")
    @PutMapping("/{id}/reject")
    public JsonData<Void> rejectArticle(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @RequestBody(required = false) String reason) {
        articleAdminBizService.rejectArticle(id, reason);
        return JsonData.buildSuccess();
    }

    /**
     * 设置/取消置顶
     */
    @RecordLog(Type = "update", businessType = "article", description = "设置/取消文章置顶")
    @Operation(summary = "设置/取消置顶", description = "管理员设置或取消文章置顶")
    @PutMapping("/{id}/top")
    public JsonData<Void> toggleTop(
            @Parameter(description = "文章ID") @PathVariable Long id,
            @RequestParam Boolean isTop) {
        articleAdminBizService.setArticleTop(id, isTop);
        return JsonData.buildSuccess();
    }
}
