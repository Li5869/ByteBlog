package com.personblog.article.vo;

import lombok.Data;

import java.util.List;

/**
 * 文章详情 VO
 * @author LSH
 */
@Data
public class ArticleDetailVO {

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章内容（Markdown格式）
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String cover;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 分类信息
     */
    private CategoryInfo category;

    /**
     * 标签列表
     */
    private List<TagInfo> tags;

    /**
     * 浏览量
     */
    private Long views;

    /**
     * 点赞数
     */
    private Long totalLikes;

    /**
     * 评论数
     */
    private Long comments;

    /**
     * 收藏数
     */
    private Long collections;

    /**
     * 当前用户是否已点赞
     */
    private Boolean isLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean isCollected;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
    /**
     * 分类信息内部类
     */
    @Data
    public static class CategoryInfo {
        /**
         * 分类ID
         */
        private Long id;

        /**
         * 分类名称
         */
        private String name;
    }

    /**
     * 标签信息内部类
     */
    @Data
    public static class TagInfo {
        /**
         * 标签ID
         */
        private Long id;

        /**
         * 标签名称
         */
        private String name;
    }
}
