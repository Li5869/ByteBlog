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
 * 可用优惠券 VO（VIP订单场景）
 * @author LSH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "可用优惠券信息")
public class AvailableCouponVO {

    @Schema(description = "用户优惠券ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "优惠券模板ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long couponTemplateId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "类型：1-满减券 2-折扣券 3-无门槛券")
    private Short couponType;

    @Schema(description = "类型文案")
    private String couponTypeText;

    @Schema(description = "优惠金额（满减券/无门槛券）")
    private Integer discountAmount;

    @Schema(description = "折扣率（折扣券，如0.85表示85折）")
    private BigDecimal discountRate;

    @Schema(description = "最低消费金额")
    private Integer minOrderAmount;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "状态：0-未使用")
    private Short status;
}
