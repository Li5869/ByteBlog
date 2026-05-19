package com.personblog.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量删除结果")
public class BatchDeleteResultVO {

    @Schema(description = "实际删除的数量")
    private Integer deletedCount;
}
