package com.personblog.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布评论请求DTO
 *
 * @author LSH
 */
@Data
@Schema(description = "发布评论请求参数")
public class CommentCreateDTO {
    @Schema(description = "冗余发送方id")
    private Long userId;
    @NotNull(message = "文章ID不能为空")
    @Schema(description = "文章ID")
    private Long articleId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", example = "这篇文章写得很好！")
    private String content;

    @Schema(description = "父评论ID，回复时必填")
    private Long parentId;

    @Schema(description = "文章标题，冗余用于发送通知")
    private String articleTitle;

    @Schema(description = "当前评论总数")
    private Long totalComment;
}
