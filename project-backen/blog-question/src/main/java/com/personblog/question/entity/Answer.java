package com.personblog.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 回答表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_answer")
public class Answer {

    /** 回答ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 回答内容 */
    private String content;

    /** 问题ID，关联tb_question */
    private Long questionId;

    /** 回答者ID，关联tb_user */
    private Long authorId;

    /** 点赞量 */
    private Long likes;

    /** 是否最佳答案 */
    private Boolean isBest;

    /** 逻辑删除标记 */
    @TableLogic
    private Boolean isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
