package com.personblog.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "我的问题查询参数")
public class MyQuestionQueryDTO {

    @Schema(description = "当前页码", example = "1")
    private Integer current;

    @Schema(description = "每页数量", example = "10")
    private Integer size;

    @Schema(description = "状态筛选：all-全部，solved-已解决，unsolved-待解决")
    private String status;

    @Schema(description = "排序方式：created_at-按时间，answers-按回答数，likes-按点赞数")
    private String orderBy;
}
