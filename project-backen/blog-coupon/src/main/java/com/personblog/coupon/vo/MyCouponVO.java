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
 * 用户优惠券列表项 VO
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户优惠券列表项")
public class MyCouponVO {

    @Schema(description = "用户优惠券ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "优惠券模板ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long couponTemplateId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "优惠券类型：1-满减券 2-折扣券 3-立减券")
    private Short couponType;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "最低消费金额")
    private BigDecimal minOrderAmount;

    @Schema(description = "最大优惠金额")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "状态：0-未使用 1-已使用 2-已过期")
    private Short status;

    @Schema(description = "来源：1-免费领取 2-积分兑换")
    private Short sourceType;

    @Schema(description = "领取时间")
    private LocalDateTime obtainTime;

    @Schema(description = "使用时间（未使用为 null）")
    private LocalDateTime useTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "使用的订单ID（未使用为 null）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @Schema(description = "优惠券描述")
    private String description;
}
