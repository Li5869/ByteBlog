package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 关注表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_follow")
public class Follow {

    /** 关注ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关注者ID，逻辑外键关联tb_user */
    private Long followerId;

    /** 被关注者ID，逻辑外键关联tb_user */
    private Long followingId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
