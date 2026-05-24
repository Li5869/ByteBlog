package com.personblog.article.vo.Article;

import lombok.Data;

import java.util.List;

/**
 * 文章基础信息 VO（可缓存）
 * 包含文章内容、分类、标签等发布后基本不变的数据
 * 互动数据（点赞/收藏/浏览/评论）不在此 VO 中，通过 ArticleInteractionVO 单独获取
 * @author LSH
 */
@Data
public class ArticleMetadataVO {

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
