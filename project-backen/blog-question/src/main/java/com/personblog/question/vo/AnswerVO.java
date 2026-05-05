package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "回答返回对象")
public class AnswerVO {
    @Schema(description = "回答ID")
    private Long id;

    @Schema(description = "回答内容（支持Markdown格式）")
    private String content;

    @Schema(description = "回答者信息")
    private AuthorInfoVO author;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "是否为最佳答案")
    private Boolean isBest;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "回答时间")
    private LocalDateTime createdAt;
}
