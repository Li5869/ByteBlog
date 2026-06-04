package com.personblog.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券表
 *
 * @author LSH
 * @since 2026-06-03
 */
@Data
@TableName("tb_user_coupon")
public class UserCoupon {
    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 优惠券模板ID */
    private Long couponTemplateId;

    /** 优惠券名称（冗余） */
    private String couponName;

    /** 类型（冗余） */
    private Short couponType;

    /** 优惠金额（冗余） */
    private BigDecimal discountAmount;

    /** 折扣率（冗余，折扣券专用） */
    private BigDecimal discountRate;

    /** 最低消费金额（冗余） */
    private BigDecimal minOrderAmount;

    /** 状态：0-未使用 1-已使用 2-已过期 3-已冻结 */
    private Short status;

    /** 来源：1-免费领取 2-积分抢购 3-系统发放 */
    private Short sourceType;

    /** 领取时间 */
    private LocalDateTime obtainTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 使用的订单ID */
    private Long orderId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否删除 */
    @TableLogic
    private Boolean isDeleted;
}
