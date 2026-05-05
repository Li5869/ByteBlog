package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的回答查询参数")
public class MyAnswerQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer current;

    @Schema(description = "每页数量", example = "10")
    private Integer size;

    @Schema(description = "类型筛选：all-全部，best-最佳答案，normal-普通回答")
    private String type;

    @Schema(description = "排序方式：created_at-按时间，likes-按点赞数")
    private String orderBy;
}
