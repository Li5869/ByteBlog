package com.personblog.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询会话列表请求")
public class ConversationQueryDTO {

    @Schema(description = "当前页", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
