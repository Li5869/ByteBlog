package com.personblog.article.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文章列表查询参数")
public class ArticleQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current = 1;

    @Schema(description = "每页数量，默认10，最大50")
    private Integer size = 10;

    @Schema(description = "分类ID，筛选指定分类")
    private Long categoryId;

    @Schema(description = "标签ID，筛选指定标签")
    private Long tagId;

    @Schema(description = "作者ID，筛选指定作者的文章")
    private Long authorId;

    @Schema(description = "是否是关注界面",defaultValue = "false")
    private Boolean Follow;

    @Schema(description = "排序字段：created_at(默认)、views、likes")
    private String orderBy = "created_at";
}
