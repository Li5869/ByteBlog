package com.personblog.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户优惠券列表查询参数 DTO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Schema(description = "用户优惠券列表查询参数")
public class MyCouponQueryDTO {

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "状态筛选：null-全部，0-未使用，1-已使用，2-已过期")
    private Integer status;
}
