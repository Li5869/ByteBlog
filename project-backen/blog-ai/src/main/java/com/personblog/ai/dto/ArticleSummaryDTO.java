package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章摘要生成请求
 *
 * @author LSH
 */
@Data
@Schema(description = "文章摘要生成请求")
public class ArticleSummaryDTO {

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容")
    private String content;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "摘要最大长度，默认200")
    private Integer maxLength = 200;
}