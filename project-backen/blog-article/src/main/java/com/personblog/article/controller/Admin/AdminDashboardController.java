package com.personblog.article.controller.Admin;

import com.personblog.article.BizService.ArticleAdminBizService;
import com.personblog.article.vo.AdminDashboardVO;
import com.personblog.article.vo.AdminRecentArticleVO;
import com.personblog.article.vo.AdminTrendsVO;
import com.personblog.common.result.JsonData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端仪表盘控制器
 * 提供全站概览统计数据、趋势图表和最近文章
 *
 * @author LSH
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "管理端-仪表盘", description = "管理后台首页仪表盘接口")
public class AdminDashboardController {

   private final ArticleAdminBizService articleAdminBizService;
    /**
     * 获取仪表盘概览统计
     * 返回文章总数、用户总数、评论总数、问答总数及环比变化
     */
    @Operation(summary = "获取仪表盘概览统计", description = "获取全站核心统计数据及环比变化")
    @GetMapping("/statistics")
    public JsonData<AdminDashboardVO> getStatistics() {
        AdminDashboardVO statistics = articleAdminBizService.getDashboardStatistics();
        return JsonData.buildSuccess(statistics);
    }

    /**
     * 获取趋势数据
     * 返回月度趋势数据，用于前端 ECharts 图表展示
     */
    @Operation(summary = "获取趋势数据", description = "获取月度趋势数据，用于图表展示")
    @GetMapping("/trends")
    public JsonData<AdminTrendsVO> getTrends(
            @Parameter(description = "年份，默认当前年") @RequestParam(required = false) Integer year) {
        AdminTrendsVO trends = articleAdminBizService.getTrendsData(year);
        return JsonData.buildSuccess(trends);
    }

    /**
     * 获取最近文章
     * 返回最近发布/更新的文章列表
     */
    @Operation(summary = "获取最近文章", description = "获取最近发布/更新的文章列表")
    @GetMapping("/recent-articles")
    public JsonData<List<AdminRecentArticleVO>> getRecentArticles(
            @Parameter(description = "返回数量，默认5") @RequestParam(defaultValue = "5") Integer size) {
        // 限制最大返回20条
        size = Math.min(size, 20);
        List<AdminRecentArticleVO> articles = articleAdminBizService.getRecentArticles(size);
        return JsonData.buildSuccess(articles);
    }
}
