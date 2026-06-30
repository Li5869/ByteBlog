package com.personblog.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专栏文章关联表
 *
 * @author LSH
 */
@Data
@TableName("tb_column_article")
public class ColumnArticle {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 专栏ID */
    private Long columnId;

    /** 文章ID */
    private Long articleId;

    /** 添加时间 */
    private LocalDateTime createdAt;
}
