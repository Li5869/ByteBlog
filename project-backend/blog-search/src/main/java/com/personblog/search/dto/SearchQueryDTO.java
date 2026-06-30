package com.personblog.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "搜索请求参数")
public class SearchQueryDTO {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "搜索类型：article-文章，author-作者，column-专栏，all-全部")
    private String type = "all";

    @Schema(description = "分类ID（文章搜索）")
    private Long categoryId;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "排序字段：relevance-相关度，time-时间，views-浏览量")
    private String orderBy = "relevance";

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 10;
}