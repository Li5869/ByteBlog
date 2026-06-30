package com.personblog.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "业务通知查询参数")
public class BizNotificationQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current = 1;

    @Schema(description = "每页数量，默认10，最大100")
    private Integer size = 10;

    @Schema(description = "行为类型筛选：like/comment/reply/follow/collection/answer")
    private String actionType;

    @Schema(description = "目标类型筛选：article/comment/question/answer/user")
    private String targetType;
}
