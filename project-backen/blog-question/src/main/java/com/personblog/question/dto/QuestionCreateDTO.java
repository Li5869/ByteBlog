package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建问题参数 DTO
 *
 * @author LSH
 */
@Data
@Schema(description = "创建问题参数")
public class QuestionCreateDTO {

    @NotBlank(message = "问题标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100字符")
    @Schema(description = "问题标题", required = true)
    private String title;

    @NotBlank(message = "问题内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过5000字符")
    @Schema(description = "问题内容（Markdown格式）", required = true)
    private String content;

    @Size(max = 5, message = "最多选择5个标签")
    @Schema(description = "标签ID数组，至少1个，最多5个")
    private List<Long> tagIds;
}
