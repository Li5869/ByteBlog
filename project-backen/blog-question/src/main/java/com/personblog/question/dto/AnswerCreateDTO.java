package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建回答参数")
public class AnswerCreateDTO {
    @NotBlank(message = "回答内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过5000字符")
    @Schema(description = "回答内容", required = true)
    private String content;
}
