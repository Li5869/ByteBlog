package com.personblog.coupon.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券详情 VO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优惠券详情")
public class CouponDetailVO {

    @Schema(description = "优惠券模板ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券类型：1-满减券 2-折扣券 3-立减券")
    private Short couponType;

    @Schema(description = "领取类型：1-免费领取 2-积分兑换")
    private Short claimType;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "最低消费金额")
    private BigDecimal minOrderAmount;

    @Schema(description = "最大优惠金额")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "兑换所需积分（免费领取时为 0）")
    private Integer pointsCost;

    @Schema(description = "抢购开始时间（null 表示上架即可抢）")
    private LocalDateTime claimStartTime;

    @Schema(description = "抢购结束时间（null 表示不限时）")
    private LocalDateTime claimEndTime;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "总库存")
    private Integer totalCount;

    @Schema(description = "有效期开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效期结束时间")
    private LocalDateTime endTime;

    @Schema(description = "有效天数")
    private Integer validDays;

    @Schema(description = "状态：0-禁用 1-启用")
    private Short status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "当前用户是否已领取")
    private Boolean claimed;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
