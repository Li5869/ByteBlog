package com.personblog.vip.bizService;

import com.personblog.api.couponAPI.CouponAPI;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.constant.OrderStatus;
import com.personblog.common.entity.Order;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.service.IOrderService;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.service.IVipPlanService;
import com.personblog.vip.vo.ConfirmOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmService {
    private final IOrderService orderService;
    private final IVipPlanService planService;
    private final CouponAPI couponAPI;
    private final PointAPI pointAPI;
    private final VipMembershipBizService membershipBizService;


    /**
     * 确认下单事务内逻辑（由 confirmOrder 通过自注入代理调用）
     * 在同一本地事务中完成：积分扣减 → 优惠券核销 → VIP激活 → 订单完成
     * 任一步失败，事务自动回滚全部撤销
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfirmOrderVO doConfirmOrder(Long orderId, Long userId) {
        // 锁内二次校验最新状态（防并发窗口内状态变化）
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
        }

        // 1. 扣减积分（原子 SQL 防超扣 + 写流水，单步替代 freeze→confirm）
        if (!pointAPI.deductPoints(userId, order.getActualPoints(),
                "VIP_PURCHASE", orderId, "VIP会员购买")) {
            throw new BizException(BizCodeEnum.POINT_NOT_ENOUGH);
        }

        // 2. 核销优惠券（直接 未使用→已使用，替代 freeze→use）
        if (order.getCouponId() != null) {
            couponAPI.useCoupon(userId, order.getCouponId(), orderId);
        }

        // 3. 激活 VIP 会员
        VipPlan plan = planService.getById(order.getBizId());
        if (plan != null) {
            membershipBizService.activateVip(userId, plan.getDurationMonths(), orderId);
        }

        // 4. 订单状态 PENDING → COMPLETED
        order.setStatus(OrderStatus.COMPLETED);
        orderService.updateById(order);
        log.info("订单确认完成: orderId={}, userId={}", orderId, userId);

        ConfirmOrderVO vo = new ConfirmOrderVO();
        vo.setOrderId(orderId);
        vo.setStatus(order.getStatus());
        vo.setActualPoints(order.getActualPoints());
        vo.setUpdatedAt(LocalDateTime.now());
        return vo;
        // 异常 → 事务自动回滚：积分/优惠券/VIP/订单 全部撤销，无需补偿
    }
}
