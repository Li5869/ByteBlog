package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 写作任务创建请求
 *
 * @author LSH
 */
@Data
@Schema(description = "写作任务创建请求")
public class WritingTaskCreateDTO {

    @Schema(description = "写作需求描述", example = "帮我写一篇关于Spring AI与LangChain对比的技术文章")
    @NotBlank(message = "写作需求不能为空")
    private String message;
}
