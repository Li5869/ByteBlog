package com.personblog.article.vo.Article;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "我的文章返回对象")
public class MyArticleVO {

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图片URL")
    private String cover;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "文章状态：0-草稿，1-已发布，2-已下架")
    private Integer status;

    @Schema(description = "审核状态：approved-通过，rejected-拒绝，pending-待审核")
    private String review;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "点赞量")
    private Long likes;

    @Schema(description = "评论量")
    private Long comments;
}
