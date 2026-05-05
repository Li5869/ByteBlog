package com.personblog.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_question_tag")
public class QuestionTag {
    /** 关联ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID(关联tb_question) */
    private Long questionId;

    /** 标签ID(关联tb_tag) */
    private Long tagId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
