package com.personblog.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "未读数量")
public class UnreadCountVO {

    @Schema(description = "未读数量")
    private Integer count;
}
