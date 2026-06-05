package com.personblog.coupon.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.UserCouponService;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
