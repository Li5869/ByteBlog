package com.personblog.coupon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户优惠券统计 VO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户优惠券统计")
public class MyCouponStatsVO {

    @Schema(description = "优惠券总数")
    private Integer total;

    @Schema(description = "未使用数量")
    private Integer unused;

    @Schema(description = "已使用数量")
    private Integer used;

    @Schema(description = "已过期数量")
    private Integer expired;
}
