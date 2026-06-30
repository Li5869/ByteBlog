package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 写作任务详情响应
 * <p>
 * 包含任务信息和最新写作计划，用于前端恢复任务到对应阶段
 * </p>
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "写作任务详情响应")
public class WritingTaskDetailVO {

    // ==================== 任务基本信息 ====================

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "用户写作需求")
    private String userRequest;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前步骤")
    private String currentStep;

    @Schema(description = "修订次数")
    private Integer revisionCount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联文章ID")
    private Long articleId;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "最终动作: publish-已发布, draft-已存草稿, null-待处理")
    private String finalAction;

    // ==================== 写作计划 ====================

    @Schema(description = "文章主题")
    private String topic;

    @Schema(description = "目标读者")
    private String targetAudience;

    @Schema(description = "写作风格")
    private String writingStyle;

    @Schema(description = "预计篇幅")
    private String estimatedLength;

    @Schema(description = "核心要点")
    private List<String> keyPoints;

    @Schema(description = "文章结构")
    private List<String> structure;

    @Schema(description = "参考关键词")
    private List<String> referenceKeywords;
}
