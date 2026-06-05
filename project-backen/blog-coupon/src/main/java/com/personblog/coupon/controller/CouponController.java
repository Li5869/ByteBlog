package com.personblog.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.coupon.bizService.CouponBizService;
import com.personblog.coupon.dto.CouponClaimDTO;
import com.personblog.coupon.dto.CouponZoneQueryDTO;
import com.personblog.coupon.vo.CouponDetailVO;
import com.personblog.coupon.vo.CouponZoneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
     * 获取优惠券模板详情
     * 已登录用户返回领取状态
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取优惠券详情", description = "获取优惠券模板详情")
    public JsonData<CouponDetailVO> getCouponDetail(@PathVariable Long id) {
        CouponDetailVO vo = couponBizService.getCouponDetail(id);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 领优惠券
     * @param dto 优惠券请求信息
     * @return 领取成功信息
     */
    @PostMapping("/claim")
    @Operation(summary = "领取优惠券(包含积分消费领取)",description = "领优惠券")
    public JsonData<Void> claimCoupon(@RequestBody CouponClaimDTO dto){
       couponBizService.claimCoupon(dto);
       return JsonData.buildSuccess();
    }
}
