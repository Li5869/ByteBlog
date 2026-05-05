package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "我的问题返回对象")
public class MyQuestionVO {

    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "问题内容")
    private String content;

    @Schema(description = "回答数量")
    private Long answers;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "点赞数量")
    private Long likes;

    @Schema(description = "是否已解决")
    private Boolean isSolved;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "标签列表")
    private List<String> tags;
}
