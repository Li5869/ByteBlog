package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI写作计划表
 *
 * @author LSH
 */
@Data
@TableName("tb_writing_plan")
public class WritingPlan {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    private Integer version;

    private String topic;

    private String targetAudience;

    private String keyPoints;

    private String writingStyle;

    private String estimatedLength;

    private String referenceKeywords;

    private String structure;

    private String approvalStatus;

    private String userFeedback;

    private LocalDateTime createdAt;
}
