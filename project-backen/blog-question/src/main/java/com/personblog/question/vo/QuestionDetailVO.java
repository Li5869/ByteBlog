package com.personblog.question.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 问题详情返回 VO
 *
 * @author LSH
 */
@Data
@Schema(description = "问题详情返回对象")
public class QuestionDetailVO {

    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "问题内容（Markdown格式）")
    private String content;

    @Schema(description = "提问者信息")
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

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;
}
