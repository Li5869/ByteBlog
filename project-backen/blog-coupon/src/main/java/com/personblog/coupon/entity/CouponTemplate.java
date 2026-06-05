package com.personblog.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板表
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@TableName("tb_coupon_template")
public class CouponTemplate {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 优惠券名称 */
    private String couponName;

    /** 类型：1-满减券 2-折扣券 3-无门槛券 */
    private Short couponType;

    /** 领取类型：1-免费领取 2-积分兑换 */
    private Short claimType;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 折扣率（折扣券专用） */
    private BigDecimal discountRate;

    /** 最低消费金额 */
    private BigDecimal minOrderAmount;

    /** 最大优惠金额（折扣券封顶） */
    private BigDecimal maxDiscountAmount;

    /** 总库存（null表示不限量） */
    private Integer totalCount;

    /** 剩余库存（不限量时为null） */
    private Integer stock;

    /** 抢购开始时间（null 表示上架即可抢） */
    private LocalDateTime claimStartTime;

    /** 抢购结束时间（null 表示不限时） */
    private LocalDateTime claimEndTime;

    /** 有效期开始时间（券本身的使用起始时间） */
    private LocalDateTime startTime;

    /** 有效期结束时间（券本身的使用截止时间） */
    private LocalDateTime endTime;

    /** 有效天数（领取后N天类型） */
    private Integer validDays;

    /** 所需积分（0或null表示免费领取） */
    private Integer pointsCost;

    /** 状态：0-下架 1-上架 */
    private Short status;

    /** 使用说明 */
    private String description;

    /** 创建者ID */
    private Long creatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否删除 */
    @TableLogic
    private Boolean isDeleted;
}
