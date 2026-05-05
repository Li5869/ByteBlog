package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 收藏表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_collection")
public class Collection {

    /** 收藏ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID，逻辑外键关联tb_user */
    private Long userId;

    /** 文章ID，逻辑外键关联tb_article */
    private Long articleId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
