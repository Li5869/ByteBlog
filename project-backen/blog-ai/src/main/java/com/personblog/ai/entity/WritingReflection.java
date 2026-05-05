package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI写作反思评价表
 * 存储最终一轮的5维度评分和改进建议
 *
 * @author LSH
 */
@Data
@TableName("tb_writing_reflection")
public class WritingReflection {

    /**
     * 评价记录ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 关联写作任务ID（tb_writing_task.id，1:1）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    /**
     * 综合评分（0.0-10.0，加权计算）
     */
    private BigDecimal score;

    /**
     * 完整性评分（权重30%，0.0-10.0）
     */
    private BigDecimal completeness;

    /**
     * 结构性评分（权重20%，0.0-10.0）
     */
    private BigDecimal structure;

    /**
     * 表达质量评分（权重25%，0.0-10.0）
     */
    private BigDecimal expression;

    /**
     * 实用性评分（权重15%，0.0-10.0）
     */
    private BigDecimal practicality;

    /**
     * 格式规范评分（权重10%，0.0-10.0）
     */
    private BigDecimal format;

    /**
     * 优点列表（JSON数组，如 ["优点1","优点2"]）
     */
    private String strengths;

    /**
     * 不足列表（JSON数组，如 ["不足1","不足2"]）
     */
    private String weaknesses;

    /**
     * 改进建议列表（JSON数组，如 ["建议1","建议2"]）
     */
    private String suggestions;

    /**
     * 最终评价时间
     */
    private LocalDateTime createdAt;
}
