package com.personblog.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端操作日志查询参数
 *
 * @author LSH
 */
@Data
@Schema(description = "管理端操作日志查询参数")
public class AdminLogQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current;

    @Schema(description = "每页条数，默认10")
    private Integer size;

    @Schema(description = "关键词，搜索操作描述和管理员名称")
    private String keyword;

    @Schema(description = "操作类型：login/logout/create/update/delete/review")
    private String actionType;

    @Schema(description = "操作对象类型：article/user/comment/question/category/tag")
    private String targetType;

    @Schema(description = "排序字段：actionType/targetType/createdAt")
    private String sortField;

    @Schema(description = "排序方式：asc/desc，默认desc")
    private String sortOrder;
}
