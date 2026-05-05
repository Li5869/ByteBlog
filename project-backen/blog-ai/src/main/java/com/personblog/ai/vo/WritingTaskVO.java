package com.personblog.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 写作任务响应
 * 
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "写作任务响应")
public class WritingTaskVO {

    @Schema(description = "任务ID", example = "writing_abc123")
    private String taskId;

    @Schema(description = "任务状态", example = "planning")
    private String status;
}
