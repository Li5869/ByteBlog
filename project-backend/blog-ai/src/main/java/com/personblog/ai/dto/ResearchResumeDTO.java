package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 研究任务恢复请求（回答澄清问题 / 确认计划 / 修改意见）
 *
 * @author LSH
 */
@Data
@Schema(description = "研究任务恢复请求")
public class ResearchResumeDTO {

    @Schema(description = "任务ID", example = "research_abc123")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "用户响应（回答澄清问题 / 确认计划 / 修改意见）", example = "approve")
    @NotBlank(message = "响应内容不能为空")
    private String response;
}
