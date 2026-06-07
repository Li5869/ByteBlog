package com.personblog.coupon.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.UserCouponService;
import com.personblog.coupon.vo.AvailableCouponVO;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyCouponBizService {

    private final UserCouponService userCouponService;

    /**
     * 获取当前用户的优惠券列表（分页）
     * 需要登录
     */
    public Page<MyCouponVO> getMyCoupons(MyCouponQueryDTO queryDTO) {
        // 获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();

        // 1. 构建分页参数
        Page<UserCoupon> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(queryDTO.getStatus() != null, UserCoupon::getStatus, queryDTO.getStatus())
                // 按领取时间倒序
                .orderByDesc(UserCoupon::getObtainTime);

        // 3. 执行分页查询
        Page<UserCoupon> userCouponPage = userCouponService.page(page, wrapper);

        // 4. 转换为 VO
        Page<MyCouponVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<MyCouponVO> voList = userCouponPage.getRecords().stream()
                .map(u -> BeanUtil.copyProperties(u, MyCouponVO.class))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }


    /**
     * 获取当前用户优惠券统计
     * 包含总数、未使用、已使用、已过期数量
     */
    public MyCouponStatsVO getMyCouponStats() {
        // 获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();

        // 分别统计各状态数量
        long total = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId).count();
        long unused = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 0).count();
        long used = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 1).count();
        long expired = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 2).count();

        return MyCouponStatsVO.builder()
                .total((int) total)
                .unused((int) unused)
                .used((int) used)
                .expired((int) expired)
                .build();
    }

    /**
     * 查询当前用户可用于VIP订单的优惠券
     * 筛选条件：未使用 + 未过期 + 满足最低消费
     *
     * @param planPrice 套餐积分价格（用于筛选满足最低消费的优惠券）
     * @return 可用优惠券列表
     */
    public List<AvailableCouponVO> getAvailableCoupons(Integer planPrice) {
        Long userId = UserContextHolder.getUserId();

        // 查询未使用、未过期、且最低消费满足当前套餐价格的优惠券
        List<UserCoupon> coupons = userCouponService.list(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getStatus, 0)  // 未使用
                        .gt(UserCoupon::getExpireTime, LocalDateTime.now())  // 未过期
                        .le(UserCoupon::getMinOrderAmount, planPrice)  // 满足最低消费
                        .orderByDesc(UserCoupon::getObtainTime)
        );

        return coupons.stream().map(c -> AvailableCouponVO.builder()
                .id(c.getId())
                .couponTemplateId(c.getCouponTemplateId())
                .couponName(c.getCouponName())
                .couponType(c.getCouponType())
                .couponTypeText(getCouponTypeText(c.getCouponType()))
                .discountAmount(c.getDiscountAmount())
                .discountRate(c.getDiscountRate())
                .minOrderAmount(c.getMinOrderAmount())
                .expireTime(c.getExpireTime())
                .status(c.getStatus())
                .build()
        ).toList();
    }

    /**
     * 优惠券类型转中文文案
     */
    private String getCouponTypeText(short couponType) {
        return switch (couponType) {
            case 1 -> "满减券";
            case 2 -> "折扣券";
            case 3 -> "无门槛券";
            default -> "未知";
        };
    }
}
