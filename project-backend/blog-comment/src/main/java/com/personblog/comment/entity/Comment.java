package com.personblog.comment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 评论表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_comment")
public class Comment {

    /** 评论ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 评论内容 */
    private String content;

    /** 文章ID，逻辑外键关联tb_article */
    private Long articleId;

    /** 评论者ID，逻辑外键关联tb_user */
    private Long authorId;

    /** 父评论ID，逻辑外键关联tb_comment，用于回复 */
    private Long parentId;

    /** 点赞量 */
    private Long likes;

    /** 逻辑删除标记: false-未删除, true-已删除 */
    @TableLogic
    private Boolean isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 审核状态: pending-待审核, approved-已通过, rejected-已拒绝 */
    private String status;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 审核人ID，逻辑外键关联tb_user */
    private Long reviewerId;

    /** 是否匿名 */
    private Boolean isAnonymous;
}
