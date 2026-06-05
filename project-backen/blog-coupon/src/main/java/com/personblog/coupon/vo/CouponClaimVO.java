package com.personblog.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 领取优惠券响应 VO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "领取优惠券响应")
public class CouponClaimVO {

    @Schema(description = "是否领取成功")
    private Boolean success;
}
