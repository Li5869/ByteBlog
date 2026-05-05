package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "热门问题简化对象")
public class HotQuestionVO {
    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "问题标题（截断至50字符）")
    private String title;

    @Schema(description = "回答数量")
    private Long answers;

    @Schema(description = "浏览量")
    private Long views;
}
