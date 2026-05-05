package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞表
 *
 * @author LSH
 */
@Data
@TableName("tb_comment_like")
public class CommentLike {

    /** 点赞ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 评论ID */
    private Long commentId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
