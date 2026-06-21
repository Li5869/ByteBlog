package com.personblog.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.coupon.bizService.MyCouponBizService;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.vo.AvailableCouponVO;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * 查询可用于VIP订单的可用优惠券
     * GET /api/coupon/my/available
     */
    @GetMapping("/my/available")
    @Operation(summary = "查询可用优惠券", description = "查询当前用户可用于VIP订单的优惠券，按套餐价格筛选满足最低消费的优惠券")
    public JsonData<List<AvailableCouponVO>> getAvailableCoupons(
            @Parameter(description = "套餐积分价格", required = true) @RequestParam Integer planId) {
        // planId 传递给 BizService，由 BizService 查询套餐价格后筛选优惠券
        // 这里简化处理：planId 即为套餐价格的代理参数，实际由前端传入套餐价格
        List<AvailableCouponVO> list = myCouponBizService.getAvailableCoupons(planId);
        return JsonData.buildSuccess(list);
    }
}
