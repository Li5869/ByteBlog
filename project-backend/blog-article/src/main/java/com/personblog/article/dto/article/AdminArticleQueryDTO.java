package com.personblog.article.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端文章查询DTO
 *
 * @author LSH
 */
@Data
@Schema(description = "管理端文章查询参数")
public class AdminArticleQueryDTO {

    @Schema(description = "当前页码")
    private Integer current;

    @Schema(description = "每页大小")
    private Integer size;

    @Schema(description = "标题关键词搜索")
    private String keyword;

    @Schema(description = "状态：all/published/draft/offline")
    private String status;

    @Schema(description = "审核状态：all/pending/approved/rejected")
    private String reviewStatus;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "排序字段：created_at/views/likes")
    private String sortField;

    @Schema(description = "排序方式：asc/desc，默认desc")
    private String sortOrder;
}
