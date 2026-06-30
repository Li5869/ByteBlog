package com.personblog.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户优惠券表 Mapper 接口
 *
 * @author LSH
 * @since 2026-06-03
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
}
