package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 研究任务启动请求
 *
 * @author LSH
 */
@Data
@Schema(description = "研究任务启动请求")
public class ResearchStartDTO {

    @Schema(description = "任务ID（前端生成的UUID）", example = "research_abc123")
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @Schema(description = "研究需求描述", example = "帮我研究一下Redis分布式锁的最佳实践")
    @NotBlank(message = "研究需求不能为空")
    private String message;
}
