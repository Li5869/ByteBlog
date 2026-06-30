package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 写作任务状态更新参数
 *
 * @author LSH
 */
@Data
@Schema(description = "写作任务状态更新参数")
public class WritingTaskUpdateDTO {

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前步骤")
    private String currentStep;
}
