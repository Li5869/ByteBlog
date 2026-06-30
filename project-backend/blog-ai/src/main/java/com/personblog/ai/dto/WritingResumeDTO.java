package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 写作任务恢复请求
 * 
 * 用户审核大纲后，通过此接口恢复任务执行
 * 
 * @author LSH
 */
@Data
@Schema(description = "写作任务恢复请求")
public class WritingResumeDTO {

    @Schema(description = "操作类型：approve-批准执行, revise-修改大纲", example = "approve", allowableValues = {"approve", "revise"})
    @NotBlank(message = "操作类型不能为空")
    private String action;

    @Schema(description = "修改意见（仅当action=revise时需要）", example = "请增加性能对比部分")
    private String feedback;
}
