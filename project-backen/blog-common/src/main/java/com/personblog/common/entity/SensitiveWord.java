package com.personblog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 敏感词表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_sensitive_word")
public class SensitiveWord {

    /** 敏感词ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 敏感词内容 */
    private String word;

    /** 状态: 0-禁用, 1-启用 */
    private Short status;

    /** 创建者ID(关联tb_user) */
    private Long creatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
