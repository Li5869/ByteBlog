package com.personblog.api.couponAPI;

import com.personblog.api.couponAPI.vo.BestCouponVO;

/**
 * 优惠券系统 API 接口
 * 用于跨模块调用优惠券服务
 *
 * @author LSH
 * @since 2026-06-06
 */
public interface CouponAPI {

    /**
     * 核销优惠券（标记为已使用）
     *
     * @param userId   用户ID
     * @param couponId 用户优惠券ID（tb_user_coupon.id）
     * @param orderId  使用的订单ID
     */
    void useCoupon(Long userId, Long couponId, Long orderId);

    BestCouponVO getTheBestCoupon(Integer price,Long userId);
    /**
     * 冻结优惠券
     */
    boolean freezeCoupon(Long couponId, Long userId);

    /**
     * 释放冻结的优惠券（冻结 → 可用）
     *
     * @param couponId 用户优惠券ID
     * @param userId   用户ID
     */
    void releaseCoupon(Long couponId, Long userId);


}
