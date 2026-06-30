package com.personblog.common.dto.MqMessage.Coupon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券领取消息 DTO
 * 用于 MQ 传递优惠券领取信息，异步写入数据库
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponClaimMessageDTO {

    /** 用户ID */
    private Long userId;

    /** 优惠券模板ID */
    private Long couponTemplateId;

    /** 优惠券名称（冗余，便于直接写入） */
    private String couponName;

    /** 优惠券类型（冗余） */
    private Short couponType;

    /** 优惠金额（积分，整数） */
    private Integer discountAmount;

    /** 折扣率（折扣券专用） */
    private BigDecimal discountRate;

    /** 最低消费金额（积分，整数） */
    private Integer minOrderAmount;

    /** 最大优惠金额（积分，整数，折扣券上限） */
    private Integer maxDiscountAmount;

    /** 兑换所需积分（免费领取时为 0 或 null） */
    private Integer pointsCost;

    /** 来源：1-免费领取 2-积分兑换 */
    private Short sourceType;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 消息创建时间 */
    private LocalDateTime createTime;
}
