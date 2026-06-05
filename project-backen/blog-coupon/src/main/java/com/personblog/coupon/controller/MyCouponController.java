package com.personblog.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.coupon.bizService.MyCouponBizService;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class MyCouponController {
    private final MyCouponBizService myCouponBizService;

    /**
     * 获取当前用户优惠券列表
     * 支持按状态筛选（未使用/已使用/已过期）
     */
    @GetMapping("/my/list")
    @Operation(summary = "获取用户优惠券列表", description = "分页查询当前用户的优惠券")
    public JsonData<Page<MyCouponVO>> getMyCoupons(MyCouponQueryDTO queryDTO) {
        Page<MyCouponVO> page = myCouponBizService.getMyCoupons(queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 获取当前用户优惠券统计
     * 返回各状态的优惠券数量
     */
    @GetMapping("/my/stats")
    @Operation(summary = "获取用户优惠券统计", description = "获取当前用户各状态优惠券数量")
    public JsonData<MyCouponStatsVO> getMyCouponStats() {
        MyCouponStatsVO vo = myCouponBizService.getMyCouponStats();
        return JsonData.buildSuccess(vo);
    }
}
