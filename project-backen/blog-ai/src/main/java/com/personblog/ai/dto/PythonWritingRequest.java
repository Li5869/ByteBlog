package com.personblog.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python写作服务请求
 * 
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Python写作服务请求")
public class PythonWritingRequest {

    @Schema(description = "任务ID")
    @JsonProperty("task_id")
    private String taskId;

    @Schema(description = "用户写作需求")
    private String message;

    @Schema(description = "操作类型：approve-批准执行, revise-修改大纲")
    private String action;

    @Schema(description = "修改意见")
    private String feedback;
}
