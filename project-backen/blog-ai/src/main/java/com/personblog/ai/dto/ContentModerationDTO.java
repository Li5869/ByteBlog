package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 内容审核请求
 *
 * @author LSH
 */
@Data
@Schema(description = "内容审核请求")
public class ContentModerationDTO {

    @NotBlank(message = "审核内容不能为空")
    @Schema(description = "待审核内容")
    private String content;

    @Schema(description = "内容类型: article/comment/question")
    private String contentType;

    @Schema(description = "业务Id")
    private Long bizId;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "内容标题")
    private String title;
}