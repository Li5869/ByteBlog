package com.personblog.vip.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改订单优惠券 DTO
 * @author LSH
 */
@Data
@Schema(description = "修改订单优惠券参数")
public class UpdateOrderCouponDTO {

    @Schema(description = "优惠券ID。不传或传 null 表示取消使用优惠券")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long couponId;
}
