package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回答点赞表
 *
 * @author LSH
 */
@Data
@TableName("tb_answer_like")
public class AnswerLike {

    /** 点赞ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 回答ID */
    private Long answerId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
