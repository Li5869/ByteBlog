package com.personblog.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 优惠券专区列表查询参数 DTO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Schema(description = "优惠券专区列表查询参数")
public class CouponZoneQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "领取类型筛选：0-全部，1-免费领取，2-积分兑换", defaultValue = "0")
    private Integer type = 0;
}
