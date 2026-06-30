package com.personblog.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 深度研究任务表
 *
 * @author LSH
 */
@Data
@TableName("tb_research_task")
public class ResearchTask {

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 任务UUID，前端生成，用于SSE连接标识
     */
    private String taskId;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 研究主题
     */
    private String topic;

    /**
     * 任务状态：pending/clarifying/planning/executing/reporting/completed/failed/stopped
     */
    private String status;

    /**
     * 研究计划（JSON格式，含tasks数组）
     */
    private String plan;

    /**
     * 澄清后的需求
     */
    private String clarifiedRequirements;

    /**
     * 用户对计划的反馈
     */
    private String userFeedback;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMsg;

    /**
     * 逻辑删除标记：false-正常/true-已删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
