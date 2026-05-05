package com.personblog.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章列表返回对象")
public class ArticleListVO {

    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图片URL")
    private String cover;

    @Schema(description = "作者信息")
    private AuthorVO author;

    @Schema(description = "分类名称")
    private String category;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签名称列表")
    private List<String> tags;

    @Schema(description = "浏览量")
    private Long views;

    @Schema(description = "点赞量")
    private Long likes;

    @Schema(description = "评论量")
    private Long comments;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "是否热门")
    private Boolean isHot;

    @Data
    @Schema(description = "作者信息")
    public static class AuthorVO {
        @Schema(description = "作者ID")
        private Long id;

        @Schema(description = "作者昵称")
        private String name;

        @Schema(description = "作者头像URL")
        private String avatar;
    }
}
