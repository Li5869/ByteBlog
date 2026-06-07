package com.personblog.vip.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.result.JsonData;
import com.personblog.vip.bizService.OrderBizService;
import com.personblog.vip.dto.CreateOrderDTO;
import com.personblog.vip.dto.OrderQueryDTO;
import com.personblog.vip.dto.UpdateOrderCouponDTO;
import com.personblog.vip.vo.ConfirmOrderVO;
import com.personblog.vip.vo.CreateOrderVO;
import com.personblog.vip.vo.OrderListVO;
import com.personblog.vip.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 * @author LSH
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "VIP订单查询接口")
@RequestMapping("/vip/orders")
public class VipOrderController {

    private final OrderBizService orderBizService;

    /**
     * 查询订单详情（含时间线、操作权限）
     * GET /api/vip/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "查询订单详情", description = "查询单笔订单详情，含TCC状态和时间线")
    public JsonData<OrderVO> getOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        OrderVO vo = orderBizService.getOrder(orderId);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 我的订单列表（分页）
     * GET /api/vip/orders
     */
    @GetMapping
    @Operation(summary = "我的订单列表", description = "分页查询当前用户的VIP订单列表")
    public JsonData<Page<OrderListVO>> getOrderList(OrderQueryDTO queryDTO) {
        Page<OrderListVO> page = orderBizService.getOrderList(queryDTO);
        return JsonData.buildSuccess(page);
    }

    /**
     * 预创建订单
     * @param dto 查询
     * @return 创建订单返回
     */
    @PostMapping
    @Operation(summary = "预创建订单",description = "用户点击确认后预创建订单")
    public JsonData<CreateOrderVO> createOrder(@RequestBody CreateOrderDTO dto){
       CreateOrderVO vo = orderBizService.createOrder(dto);
       return JsonData.buildSuccess(vo);
    }
    /**
     * 修改订单信息
     * @param dto 订单修改参数
     * @return 订单创建
     */
    @PutMapping("/{orderId}")
    @Operation(summary = "修改订单优惠券", description = "用户更换/取消优惠券，仅待确认状态可修改")
    public JsonData<CreateOrderVO> updateOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @RequestBody UpdateOrderCouponDTO dto) {
        CreateOrderVO vo = orderBizService.updateOrderCoupon(orderId, dto);
        return JsonData.buildSuccess(vo);
    }

    /**
     * 下单购买
     * @param orderId 订单id
     * @return 确认
     */
    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "下单优惠券",description = "用户确认下单")
    public JsonData<ConfirmOrderVO> confirmOrder(@PathVariable Long orderId){
       ConfirmOrderVO vo = orderBizService.confirmOrder(orderId);
       return JsonData.buildSuccess(vo);
    }


}
