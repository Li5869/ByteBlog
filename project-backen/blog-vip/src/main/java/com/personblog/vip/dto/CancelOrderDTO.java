package com.personblog.vip.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消订单 DTO
 * @author LSH
 */
@Data
@Schema(description = "取消订单参数")
public class CancelOrderDTO {

    @Schema(description = "取消原因", defaultValue = "用户主动取消")
    private String reason = "用户主动取消";
}
