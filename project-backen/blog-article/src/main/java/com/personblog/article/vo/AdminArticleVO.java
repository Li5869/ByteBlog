package com.personblog.article.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.personblog.common.vo.TagVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端文章VO
 *
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理端文章详情")
public class AdminArticleVO {

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "作者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "作者头像URL")
    private String authorAvatar;

    @Schema(description = "分类ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "状态：published/draft/offline")
    private String status;

    @Schema(description = "审核状态：pending/approved/rejected")
    private String reviewStatus;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "点赞数")
    private Long likes;

    @Schema(description = "评论数")
    private Long comments;

    @Schema(description = "收藏数")
    private Long collections;

    @Schema(description = "封面URL")
    private String cover;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章正文Markdown内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
