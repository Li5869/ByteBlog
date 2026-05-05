package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问题创建结果 VO
 *
 * @author LSH
 */
@Data
@Schema(description = "问题创建结果")
public class QuestionCreateVO {

    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "作者信息")
    private AuthorInfoVO author;

    @Schema(description = "标签列表")
    private List<TagInfo> tags;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "回答数量")
    private Long answers;

    @Schema(description = "点赞数量")
    private Long likes;

    @Schema(description = "是否已解决")
    private Boolean isSolved;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
