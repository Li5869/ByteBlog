package com.personblog.coupon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.mapper.CouponTemplateMapper;
import com.personblog.coupon.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 优惠券模板表 服务实现类
 *
 * @author LSH
 * @since 2026-06-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {
}
