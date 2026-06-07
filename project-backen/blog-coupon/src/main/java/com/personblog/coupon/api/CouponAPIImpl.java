package com.personblog.coupon.api;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.couponAPI.CouponAPI;
import com.personblog.api.couponAPI.vo.BestCouponVO;
import com.personblog.coupon.bizService.CouponBizService;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * CouponAPI 实现类
 *
 * @author LSH
 * @since 2026-06-06
 */
@Service
@RequiredArgsConstructor
public class CouponAPIImpl implements CouponAPI {

    private final CouponBizService couponBizService;
    private final UserCouponService userCouponService;

    @Override
    public void useCoupon(Long userId, Long couponId, Long orderId) {
        couponBizService.useCoupon(userId, couponId, orderId);
    }

    @Override
    public BestCouponVO getTheBestCoupon(Integer price, Long userId) {
        List<UserCoupon> coupons = userCouponService.list(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .eq(UserCoupon::getIsDeleted, false)
                .le(UserCoupon::getMinOrderAmount, price));
        if (CollectionUtil.isEmpty(coupons)) {
            return null;
        }
        // 直接串行计算，找出优惠金额最大的优惠券（calcDiscount 是简单算术，并行开销反而更大）
        UserCoupon bestCoupon = coupons.stream()
                .max(Comparator.comparingInt(c -> calcDiscount(c, price)))
                .orElse(null);
        if (bestCoupon == null) {
            return null;
        }
        int bestDiscount = calcDiscount(bestCoupon, price);
        BestCouponVO vo = new BestCouponVO();
        vo.setCouponId(bestCoupon.getId());
        vo.setCouponName(bestCoupon.getCouponName());
        vo.setCouponDiscount((long) bestDiscount);
        return vo;
    }

    @Override
    public boolean freezeCoupon(Long couponId, Long userId) {
        return userCouponService.lambdaUpdate()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, 0)
                .set(UserCoupon::getStatus, 3)
                .update();
    }

    @Override
    public void releaseCoupon(Long couponId, Long userId) {
        userCouponService.lambdaUpdate()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, 3)
                .set(UserCoupon::getStatus, 0)
                .update();
    }

    /**
     * 计算单张优惠券的实际优惠金额
     * 满减券/无门槛券：直接取 discountAmount
     * 折扣券：price * (1 - discountRate)，受 maxDiscountAmount 封顶
     */
    private int calcDiscount(UserCoupon coupon, int price) {
        // 满减券(1) 和 无门槛券(3) 直接返回优惠金额
        if (coupon.getCouponType() == 1 || coupon.getCouponType() == 3) {
            return coupon.getDiscountAmount();
        }
        // 折扣券(2)：优惠金额 = 价格 * (1 - 折扣率)
        if (coupon.getCouponType() == 2) {
            int discount = BigDecimal.valueOf(price)
                    .multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()))
                    .intValue();
            Integer cap = coupon.getMaxDiscountAmount();
            return cap != null ? Math.min(discount, cap) : discount;
        }
        return 0;
    }
}
