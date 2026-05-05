package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章标题生成请求
 *
 * @author LSH
 */
@Data
@Schema(description = "文章标题生成请求")
public class ArticleTitleDTO {

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容", required = true)
    private String content;

    @Schema(description = "标题最大长度，默认30")
    private Integer maxLength = 30;

    @Schema(description = "生成风格: professional/casual/creative")
    private String style = "professional";
}