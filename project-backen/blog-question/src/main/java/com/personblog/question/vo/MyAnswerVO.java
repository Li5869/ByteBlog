package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "我的回答返回对象")
public class MyAnswerVO {

    @Schema(description = "回答ID")
    private Long id;

    @Schema(description = "问题ID")
    private Long questionId;

    @Schema(description = "问题标题")
    private String questionTitle;

    @Schema(description = "回答内容")
    private String content;

    @Schema(description = "点赞数量")
    private Long likes;

    @Schema(description = "是否最佳答案")
    private Boolean isBest;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
