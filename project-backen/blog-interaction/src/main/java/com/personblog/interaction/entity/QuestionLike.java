package com.personblog.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题点赞表
 *
 * @author LSH
 */
@Data
@TableName("tb_question_like")
public class QuestionLike {

    /** 点赞ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 问题ID */
    private Long questionId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
