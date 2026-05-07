package com.personblog.article.vo;

import lombok.Data;

/**
 * 文章互动数据 VO（实时查询，不缓存）
 * 包含浏览量、点赞、收藏、评论等频繁变动的数据
 * 通过单独的接口获取，可与文章基础信息并行请求
 * @author LSH
 */
@Data
public class ArticleInteractionVO {

    /**
     * 文章ID
     */
    private Long id;

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
}
