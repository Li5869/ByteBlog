package com.personblog.coupon.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Schema(description = "用户优惠券ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userCouponId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
