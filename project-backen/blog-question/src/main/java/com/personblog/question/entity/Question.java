package com.personblog.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 问题表
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Data
@TableName("tb_question")
public class Question {

    /** 问题ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 问题标题 */
    private String title;

    /** 问题内容 */
    private String content;

    /** 提问者ID，关联tb_user */
    private Long authorId;

    /** 浏览量 */
    private Long views;

    /** 回答量 */
    private Long answers;

    /** 点赞量 */
    private Long likes;

    /** 是否已解决 */
    private Boolean isSolved;

    /** 逻辑删除标记 */
    @TableLogic
    private Boolean isDeleted;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
