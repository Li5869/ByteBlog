package com.personblog.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.coupon.bizService.CouponBizService;
import com.personblog.coupon.dto.CouponZoneQueryDTO;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.vo.CouponDetailVO;
import com.personblog.coupon.vo.CouponZoneVO;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券系统控制器
 *
 * @author LSH
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
@Tag(name = "优惠券系统", description = "优惠券查询、领取等接口")
public class CouponController {

    private final CouponBizService couponBizService;

    /**
     * 获取优惠券专区列表
     * 支持按类型筛选，已登录用户返回领取状态
     */
    @GetMapping("/zone/list")
    @Operation(summary = "获取优惠券专区列表", description = "分页查询可领取的优惠券列表")
    public JsonData<Page<CouponZoneVO>> getZoneList(CouponZoneQueryDTO queryDTO) {
        Page<CouponZoneVO> page = couponBizService.getZoneList(queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取当前用户优惠券列表
     * 支持按状态筛选（未使用/已使用/已过期）
     */
    @GetMapping("/my/list")
    @Operation(summary = "获取用户优惠券列表", description = "分页查询当前用户的优惠券")
    public JsonData<Page<MyCouponVO>> getMyCoupons(MyCouponQueryDTO queryDTO) {
        Page<MyCouponVO> page = couponBizService.getMyCoupons(queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取当前用户优惠券统计
     * 返回各状态的优惠券数量
     */
    @GetMapping("/my/stats")
    @Operation(summary = "获取用户优惠券统计", description = "获取当前用户各状态优惠券数量")
    public JsonData<MyCouponStatsVO> getMyCouponStats() {
        MyCouponStatsVO vo = couponBizService.getMyCouponStats();
        return JsonData.buildSuccess(vo);
    }

    /**
     * 获取优惠券模板详情
     * 已登录用户返回领取状态
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取优惠券详情", description = "获取优惠券模板详情")
    public JsonData<CouponDetailVO> getCouponDetail(@PathVariable Long id) {
        CouponDetailVO vo = couponBizService.getCouponDetail(id);
        return JsonData.buildSuccess(vo);
    }
}
