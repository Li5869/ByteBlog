package com.personblog.coupon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.mapper.UserCouponMapper;
import com.personblog.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户优惠券表 服务实现类
 *
 * @author LSH
 * @since 2026-06-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements UserCouponService {
}
