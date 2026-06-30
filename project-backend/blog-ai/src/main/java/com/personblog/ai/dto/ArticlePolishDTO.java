package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章润色请求
 *
 * @author LSH
 */
@Data
@Schema(description = "文章润色请求")
public class ArticlePolishDTO {

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容")
    private String content;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "润色风格：professional(专业)、friendly(友好)、concise(简洁)，默认professional")
    private String style = "professional";
}
