package com.personblog.vip.bizService;

import com.personblog.api.couponAPI.CouponAPI;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class CompensationService {
    private final CouponAPI couponAPI;
    private final PointAPI pointAPI;
    private final VipMembershipBizService membershipBizService;
    //confirm失败补偿
    public void compensateConfirmFailure(Long userId,
                                          Order order,
                                          String tccXid,
                                          boolean isVip,
                                          boolean useCoupon,
                                          boolean deductPoint) {
        // 逆序补偿：优惠券 → 积分 → VIP
        if (useCoupon && order.getCouponId() != null) {
            try {
                couponAPI.refundCoupon(order.getCouponId(), userId);
            } catch (Exception e) {
                log.error("补偿：退回优惠券异常，需人工处理, orderId={}", order.getId(), e);
            }
        }
        if (deductPoint) {
            try {
                pointAPI.refundPoints(userId, order.getActualPoints(),
                        "VIP_PURCHASE_CANCEL", order.getId(), "VIP购买失败退回");
            } catch (Exception e) {
                log.error("补偿：退回积分异常，需人工处理, orderId={}", order.getId(), e);
            }
        }
        if (isVip) {
            try {
                membershipBizService.deactivateVip(userId);
            } catch (Exception e) {
                log.error("补偿：回退VIP异常，需人工处理, orderId={}", order.getId(), e);
            }
        }
    }
}
