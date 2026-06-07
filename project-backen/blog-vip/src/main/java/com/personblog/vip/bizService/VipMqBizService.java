package com.personblog.vip.bizService;

import com.personblog.api.couponAPI.CouponAPI;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.constant.OrderStatus;
import com.personblog.common.dto.MqMessage.Vip.OrderConfirmMessageDTO;
import com.personblog.common.entity.Order;
import com.personblog.common.service.IOrderService;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.service.IVipPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.personblog.common.constant.OrderStatus.FROZEN;
import static com.personblog.vip.constant.RedisKeys.getConfirmOrderLockKey;

/**
 * MQ 消息业务处理层
 *
 * @author LSH
 * @since 2026-06-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipMqBizService {

    private final IOrderService orderService;
    private final IVipPlanService planService;
    private final PointAPI pointAPI;
    private final CouponAPI couponAPI;
    private final VipMembershipBizService membershipBizService;
    private final RedissonClient redissonClient;

    /**
     * 订单超时处理：释放资源，关闭订单
     *
     * @param orderId 订单ID
     */
    public void handleOrderTimeout(Long orderId) throws InterruptedException {
        String lockKey = getConfirmOrderLockKey(orderId);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("超时处理：获取锁失败，跳过本次处理, orderId={}", orderId);
                return;
            }

            Order order = orderService.getById(orderId);
            if (order == null) {
                log.warn("超时处理：订单不存在, orderId={}", orderId);
                return;
            }
            // 幂等：只处理待确认或已冻结状态
            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != FROZEN) {
                log.info("超时处理：订单状态无需处理，跳过, orderId={}, status={}",
                        orderId, order.getStatus());
                return;
            }

            // Cancel阶段：释放FROZEN状态下的冻结资源
            if (order.getStatus() == FROZEN) {
                cancelFrozenResources(order);
            }

            // 更新订单状态为已关闭
            short oldStatus = order.getStatus();
            order.setStatus(OrderStatus.CLOSED);
            orderService.updateById(order);
            log.info("订单超时取消成功: orderId={}, 原状态={}", orderId, oldStatus);
        } catch (Exception e) {
            log.error("超时处理异常: orderId={}", orderId, e);
            throw e;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 订单确认处理（兜底路径）：进程崩溃补偿重试
     * 正常情况下由 confirmOrder 同步完成，MQ 消息仅在崩溃场景补位
     *
     * @param msg 订单确认消息
     */
    public void handleOrderConfirm(OrderConfirmMessageDTO msg) throws InterruptedException {
        Long orderId = msg.getOrderId();
        String lockKey = getConfirmOrderLockKey(orderId);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("确认处理：获取锁失败，主流程进行中，跳过, orderId={}", orderId);
                return;
            }

            Order order = orderService.getById(orderId);
            if (order == null) {
                log.warn("确认处理：订单不存在, orderId={}", orderId);
                return;
            }
            // 幂等：只处理 FROZEN 状态（正常流程已变为 COMPLETED/CLOSED）
            if (order.getStatus() != FROZEN) {
                log.info("确认处理：订单状态非FROZEN，跳过, orderId={}, status={}", orderId, order.getStatus());
                return;
            }

            // 兜底执行 Confirm 流程
            // 1. 确认扣减积分（冻结 → 实扣）
            pointAPI.confirmDeductPoints(order.getUserId(), order.getActualPoints(),
                    "VIP_PURCHASE", orderId, "VIP会员购买");

            // 2. 核销优惠券
            if (order.getCouponId() != null) {
                couponAPI.useCoupon(order.getUserId(), order.getCouponId(), orderId);
            }

            // 3. 激活VIP会员
            VipPlan plan = planService.getById(order.getBizId());
            if (plan != null) {
                membershipBizService.activateVip(order.getUserId(), plan.getDurationMonths(), orderId);
            }

            // 4. 更新订单状态为已完成
            order.setStatus(OrderStatus.COMPLETED);
            orderService.updateById(order);
            log.info("订单兜底确认完成: orderId={}, userId={}", orderId, msg.getUserId());
        } catch (Exception e) {
            // Confirm失败：释放冻结资源 + 标记订单关闭（避免重复执行）
            log.error("兜底Confirm阶段异常，Cancel回滚资源, orderId={}", orderId, e);
            Order order = orderService.getById(orderId);
            if (order != null) {
                cancelFrozenResources(order);
                order.setStatus(OrderStatus.CLOSED);
                orderService.updateById(order);
            }
            throw e;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Cancel阶段：释放冻结资源（积分、优惠券）
     * 用于订单超时或确认失败时回滚已冻结的资源
     *
     * @param order 订单信息
     */
    private void cancelFrozenResources(Order order) {
        Long orderId = order.getId();
        Long userId = order.getUserId();

        // 释放冻结的积分
        try {
            pointAPI.cancelDeductPoints(userId, order.getActualPoints());
            log.info("Cancel阶段：释放冻结积分成功, orderId={}, points={}", orderId, order.getActualPoints());
        } catch (Exception e) {
            log.error("Cancel阶段：释放冻结积分异常，需人工处理, orderId={}, points={}", orderId, order.getActualPoints(), e);
        }

        // 释放冻结的优惠券
        if (order.getCouponId() != null) {
            try {
                couponAPI.releaseCoupon(order.getCouponId(), userId);
                log.info("Cancel阶段：释放冻结优惠券成功, orderId={}, couponId={}", orderId, order.getCouponId());
            } catch (Exception e) {
                log.error("Cancel阶段：释放冻结优惠券异常，需人工处理, orderId={}, couponId={}", orderId, order.getCouponId(), e);
            }
        }
    }
}
