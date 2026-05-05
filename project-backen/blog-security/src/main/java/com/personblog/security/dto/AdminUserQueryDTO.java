package com.personblog.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端用户查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "管理端用户查询参数")
public class AdminUserQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current;

    @Schema(description = "每页条数，默认10")
    private Integer size;

    @Schema(description = "关键词，搜索用户名、昵称或邮箱")
    private String keyword;

    @Schema(description = "状态筛选：all/normal/banned")
    private String status;

    @Schema(description = "排序字段：created_at/articles_count/fans_count")
    private String sortField;

    @Schema(description = "排序方式：asc/desc，默认desc")
    private String sortOrder;
}
