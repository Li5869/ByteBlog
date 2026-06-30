package com.personblog.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写作任务列表响应
 * <p>
 * 用于返回当前用户的所有写作任务列表，包含任务详情和进度信息
 * </p>
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "写作任务列表响应")
public class WritingTaskListVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "任务ID", example = "1234567890123456789")
    private Long id;

    @Schema(description = "用户写作需求", example = "写一篇关于 Spring Boot 自动装配原理的文章")
    private String userRequest;

    @Schema(description = "任务状态", example = "planning")
    private String status;

    @Schema(description = "当前步骤", example = "generating_plan")
    private String currentStep;

    @Schema(description = "修订次数", example = "2")
    private Integer revisionCount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联文章ID", example = "1234567890123456789")
    private Long articleId;

    @Schema(description = "创建时间", example = "2026-04-26 10:30:00")
    private String createdAt;

    @Schema(description = "完成时间", example = "2026-04-26 10:45:00")
    private String completedAt;

    @Schema(description = "错误信息", example = "AI 服务暂时不可用")
    private String errorMessage;

    @Schema(description = "最终动作: publish-已发布, draft-已存草稿, null-待处理", example = "publish")
    private String finalAction;
}
