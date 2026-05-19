package com.personblog.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量删除请求参数")
public class BatchDeleteDTO {

    @Schema(description = "通知ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
