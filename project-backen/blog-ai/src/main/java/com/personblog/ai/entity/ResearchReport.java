package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 深度研究报告表
 *
 * @author LSH
 */
@Data
@TableName("tb_research_report")
public class ResearchReport {

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 关联的研究任务UUID
     */
    private String taskId;

    /**
     * 用户ID（冗余字段，便于查询）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 研究主题
     */
    private String topic;

    /**
     * OSS报告文件地址（Markdown格式）
     */
    private String reportUrl;

    /**
     * 报告摘要（研究面板 + 历史列表展示用）
     */
    private String summary;

    /**
     * 关键发现列表（JSON格式）
     */
    private String keyFindings;

    /**
     * 引用来源（JSON格式）
     */
    private String sources;

    /**
     * 逻辑删除标记：false-正常/true-已删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
