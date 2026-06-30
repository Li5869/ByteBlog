package com.personblog.common.dto.Notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量删除请求参数")
public class BatchDeleteDTO {

    @Schema(description = "ID数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
