package com.personblog.interaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统通知查询参数")
public class SystemNotificationQueryDTO {

    @Schema(description = "当前页码，默认1")
    private Integer current = 1;

    @Schema(description = "每页数量，默认10，最大100")
    private Integer size = 10;
}
