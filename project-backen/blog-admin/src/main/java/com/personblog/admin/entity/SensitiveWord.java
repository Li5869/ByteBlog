package com.personblog.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词表
 *
 * @author LSH
 */
@Data
@TableName("tb_sensitive_word")
public class SensitiveWord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String word;

    private Short status;

    private Long creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
