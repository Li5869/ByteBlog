package com.personblog.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 文章表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_article")
public class Article {

    /** 文章ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章摘要 */
    private String summary;

    /** 文章内容 */
    private String content;

    /** 封面图片URL */
    private String cover;

    /** 作者ID(关联tb_user) */
    private Long authorId;

    /** 分类ID(关联tb_category) */
    private Long categoryId;

    /** 浏览量 */
    private Long views;

    /** 点赞量 */
    private Long likes;

    /** 评论量 */
    private Long comments;

    /** 收藏量 */
    private Long collections;

    /** 是否置顶 */
    private Boolean isTop;

    /** 是否热门 */
    private Boolean isHot;

    /** 状态: 0-草稿, 1-已发布, 2-已下架 */
    private Integer status;

    /** 逻辑删除标记 */
    @TableLogic
    private Boolean isDeleted;

    /**审核状态 approved-通过,rejected-拒绝,pending-待审核*/
    private String review;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
