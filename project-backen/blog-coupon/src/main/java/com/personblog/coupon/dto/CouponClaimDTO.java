package com.personblog.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 领取优惠券请求参数 DTO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Schema(description = "领取优惠券请求参数")
public class CouponClaimDTO {

    @Schema(description = "优惠券模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long couponTemplateId;
}
