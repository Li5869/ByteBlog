package com.personblog.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 文章标签关联表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_article_tag")
public class ArticleTag {

    /** 关联ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID(关联tb_article) */
    private Long articleId;

    /** 标签ID(关联tb_tag) */
    private Long tagId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
