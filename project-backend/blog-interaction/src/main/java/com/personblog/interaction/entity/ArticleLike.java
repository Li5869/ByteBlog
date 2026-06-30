package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章点赞表
 *
 * @author LSH
 */
@Data
@TableName("tb_article_like")
public class ArticleLike {

    /** 点赞ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 文章ID */
    private Long articleId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
