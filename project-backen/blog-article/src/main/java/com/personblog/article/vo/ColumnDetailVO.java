package com.personblog.article.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专栏详情返回对象
 *
 * @author LSH
 */
@Data
@Builder
@Schema(description = "专栏详情返回对象")
public class ColumnDetailVO {

    @Schema(description = "专栏ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "专栏标题")
    private String title;

    @Schema(description = "专栏描述")
    private String description;

    @Schema(description = "专栏封面URL")
    private String cover;

    @Schema(description = "文章数量")
    private Integer articlesCount;

    @Schema(description = "订阅数量")
    private Integer subscriptionCount;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "状态：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "作者ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "当前用户是否是作者")
    private Boolean isAuthor;

    @Schema(description = "当前用户是否已订阅")
    private Boolean isSubscribed;

    @Schema(description = "专栏文章列表")
    private List<ColumnArticleVO> articles;

    /**
     * 专栏文章项
     */
    @Data
    @Builder
    @Schema(description = "专栏文章项")
    public static class ColumnArticleVO {

        @Schema(description = "文章ID")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @Schema(description = "文章标题")
        private String title;

        @Schema(description = "文章摘要")
        private String summary;

        @Schema(description = "封面图片URL")
        private String cover;

        @Schema(description = "浏览量")
        private Long views;

        @Schema(description = "点赞量")
        private Long likes;

        @Schema(description = "评论量")
        private Long comments;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
