package com.personblog.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端评论查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "管理端评论查询参数")
public class AdminCommentQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current;

    @Schema(description = "每页条数，默认10")
    private Integer size;

    @Schema(description = "关键词，搜索评论内容或评论者名称")
    private String keyword;

    @Schema(description = "审核状态筛选：all/pending/approved/rejected")
    private String status;

    @Schema(description = "评论类型筛选：all/article（当前仅支持文章评论）")
    private String targetType;

    @Schema(description = "排序字段：created_at/likes")
    private String sortField;

    @Schema(description = "排序方式：asc/desc，默认desc")
    private String sortOrder;
}
