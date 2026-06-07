package com.personblog.vip.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建预订单 DTO（后端自动匹配最优优惠券，无需前端传 couponId）
 * @author LSH
 */
@Data
@Schema(description = "创建预订单参数")
public class CreateOrderDTO {

    @NotNull(message = "套餐ID不能为空")
    @Schema(description = "套餐ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long planId;
}
