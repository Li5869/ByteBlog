package com.personblog.article.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.article.dto.AdminArticleQueryDTO;
import com.personblog.article.dto.ArticlePublishDTO;
import com.personblog.article.entity.Article;
import com.personblog.article.vo.*;
import com.personblog.common.dto.Article.ArticleQueryDTO;

import java.util.List;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
public interface IArticleService extends IService<Article> {

    /**
     * 获取轮播图数据（热门推荐文章）
     * @param size 返回数量
     * @return 轮播图列表
     */
    List<BannerVO> getBanners(Integer size);

    /**
     * 分页查询文章列表
     * @param queryDTO 查询参数
     * @return 分页文章列表
     */
    Page<ArticleListVO> getArticlePage(ArticleQueryDTO queryDTO);

    /**
     * 获取随机文章列表
     * @param size 返回数量
     * @return 随机文章列表
     */
    List<ArticleListVO> getRandomArticles(Integer size);

    /**
     * 获取热门文章列表（按浏览量排序）
     * @param size 返回数量
     * @return 热门文章列表
     */
    List<HotArticleVO> getHotArticles(Integer size);
    /**
     * 获取文章基础信息（含正文、分类、标签）
     * 此数据变化频率低，会进行多级缓存
     * @param id 文章ID
     * @return 文章基础信息
     */
    ArticleMetadataVO getArticleMetadata(Long id);

    /**
     * 获取文章互动数据（浏览量、点赞、收藏、评论）
     * 此数据变化频繁，实时查询不缓存
     * @param id 文章ID
     * @return 文章互动数据
     */
    ArticleInteractionVO getArticleInteraction(Long id);

    /**
     * 获取相关文章列表
     * 根据文章分类和标签获取相关文章推荐
     * @param articleId 当前文章ID
     * @param limit 返回数量，默认3，最大10
     * @return 相关文章列表
     */
    List<RelatedArticleVO> getRelatedArticles(Long articleId, Integer limit);

    /**
     * 获取当前用户的文章列表
     * @param userId 用户ID
     * @param current 当前页码
     * @param size 每页大小
     * @param status 文章状态筛选（可选）
     * @param orderBy 排序字段：created_at-按时间，likes-按点赞数，views-按阅读数
     * @return 分页文章列表
     */
    Page<MyArticleVO> getMyArticles(Long userId, Integer current, Integer size, Integer status, String orderBy);

    /**
     * 获取文章编辑回填数据（仅作者）
     * @param userId 当前用户ID
     * @param articleId 文章ID
     * @return 编辑回填数据
     */
    ArticleEditVO getEditArticle(Long userId, Long articleId);

    /**
     * 创建文章（草稿/发布）
     * @param userId 当前用户ID
     * @param dto 创建参数
     * @return 创建结果
     */
    ArticlePublishVO createArticle(Long userId, ArticlePublishDTO dto);

    /**
     * 更新文章（草稿/发布）
     * @param userId 当前用户ID
     * @param articleId 文章ID
     * @param dto 更新参数
     * @return 更新结果
     */
    ArticlePublishVO updateArticle(Long userId, Long articleId, ArticlePublishDTO dto);

    /**
     * 删除文章（逻辑删除）
     * @param userId 当前用户ID
     * @param articleId 文章ID
     */
    void deleteArticle(Long userId, Long articleId);

    // ==================== 管理端接口 ====================

    /**
     * 获取仪表盘概览统计
     * @return 统计数据
     */
    AdminDashboardVO getDashboardStatistics();

    /**
     * 获取趋势数据
     * @param year 年份，默认当前年
     * @return 趋势数据
     */
    AdminTrendsVO getTrendsData(Integer year);

    /**
     * 获取最近文章
     * @param size 返回数量
     * @return 最近文章列表
     */
    List<AdminRecentArticleVO> getRecentArticles(Integer size);

    /**
     * 管理端分页查询文章列表
     * @param dto 查询参数
     * @return 分页文章列表
     */
    Page<AdminArticleVO> getAdminArticlePage(AdminArticleQueryDTO dto);

    /**
     * 管理端获取文章详情
     * @param id 文章ID
     * @return 文章详情
     */
    AdminArticleVO getAdminArticleDetail(Long id);

    /**
     * 管理端删除文章
     * @param id 文章ID
     */
    void deleteArticleByAdmin(Long id);

    /**
     * 审核通过文章
     * @param id 文章ID
     */
    void approveArticle(Long id);

    /**
     * 审核拒绝文章
     * @param id 文章ID
     * @param reason 拒绝原因
     */
    void rejectArticle(Long id, String reason);

    /**
     * 设置文章置顶状态
     * @param id 文章ID
     * @param isTop 是否置顶
     */
    void setArticleTop(Long id, Boolean isTop);
}
