package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端问题查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "管理端问题查询参数")
public class AdminQuestionQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current;

    @Schema(description = "每页条数，默认10")
    private Integer size;

    @Schema(description = "关键词，搜索问题标题")
    private String keyword;

    @Schema(description = "状态筛选：all/pending/solved")
    private String status;

    @Schema(description = "排序字段：created_at/view_count/answer_count")
    private String sortField;

    @Schema(description = "排序方式：asc/desc，默认desc")
    private String sortOrder;
}
