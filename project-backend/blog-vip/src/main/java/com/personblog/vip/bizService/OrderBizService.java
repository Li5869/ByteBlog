package com.personblog.vip.bizService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.couponAPI.CouponAPI;
import com.personblog.api.couponAPI.vo.BestCouponVO;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.constant.BizType;
import com.personblog.common.constant.OrderStatus;
import com.personblog.common.entity.Order;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.service.IOrderService;
import com.personblog.common.utils.OrderNoUtil;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.vip.dto.CreateOrderDTO;
import com.personblog.vip.dto.OrderQueryDTO;
import com.personblog.vip.dto.UpdateOrderCouponDTO;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.service.IVipPlanService;
import com.personblog.vip.tcc.TccTransactionManager;
import com.personblog.vip.util.OrderUtil;
import com.personblog.vip.vo.ConfirmOrderVO;
import com.personblog.vip.vo.CreateOrderVO;
import com.personblog.vip.vo.OrderListVO;
import com.personblog.vip.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.personblog.common.constant.OrderStatus.FROZEN;
import static com.personblog.common.constant.TccBizType.COUPON_FREEZE;
import static com.personblog.common.constant.TccBizType.POINT_FREEZE;
import static com.personblog.common.enums.BizCodeEnum.VIP_ERROR;
import static com.personblog.vip.config.mqConfig.VipOrderMqConfig.*;
import static com.personblog.vip.constant.RedisKeys.*;
import static com.personblog.vip.constant.TccXid.xid;


/**
 * 订单业务编排层
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBizService {

    private final IOrderService orderService;
    private final StringRedisTemplate redisTemplate;
    private final IVipPlanService planService;
    private final CouponAPI couponAPI;
    private final RedissonClient redissonClient;
    private final PointAPI pointAPI;
    private final RabbitTemplate rabbitTemplate;
    private final TccTransactionManager tccManager;
    private final CompensationService compensationService;
    private final VipMembershipBizService membershipBizService;

    /**
     * 查询订单详情（含时间线、操作权限）
     */
    public OrderVO getOrder(Long orderId) {
        Long userId = UserContextHolder.getUserId();
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        // 校验订单归属，防止越权查看他人订单
        if (!order.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        OrderVO vo = BeanUtil.copyProperties(order, OrderVO.class);
        vo.setTimeline(OrderUtil.buildTimeline(order));
        return vo;
    }

    /**
     * 分页查询当前用户订单列表
     */
    public Page<OrderListVO> getOrderList(OrderQueryDTO queryDTO) {
        Long userId = UserContextHolder.getUserId();

        // 构建查询条件：当前用户 + VIP业务类型 + 可选状态筛选
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getBizType, BizType.VIP)
                .eq(queryDTO.getStatus() != null, Order::getStatus, queryDTO.getStatus())
                .orderByDesc(Order::getCreatedAt);

        Page<Order> page = orderService.page(
                new Page<>(queryDTO.getCurrent(), queryDTO.getSize()),
                wrapper
        );

        // 转换为列表 VO 分页
        Page<OrderListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(o -> BeanUtil.copyProperties(o, OrderListVO.class))
                .toList());
        return voPage;
    }

    public CreateOrderVO createOrder(CreateOrderDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Long planId = dto.getPlanId();
        String repeatKey = getOrderRepeatKey(userId, planId);

        // 1. 防重复下单：Redis SETNX 5秒锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(repeatKey, "1", 5, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new BizException(BizCodeEnum.ORDER_REPEAT);
        }

        // 2. 校验套餐存在且上架
        VipPlan vipPlan = planService.getById(planId);
        if (vipPlan == null || vipPlan.getStatus() == 0) {
            throw new BizException(BizCodeEnum.VIP_PLAN_NOT_EXIST);
        }

        // 3. 获取最优优惠券，计算价格
        Integer price = vipPlan.getPointsPrice();
        BestCouponVO bestCoupon = couponAPI.getTheBestCoupon(price, userId);
        int couponDiscount = (bestCoupon != null) ? bestCoupon.getCouponDiscount().intValue() : 0;
        int actualPoints = Math.max(0, price - couponDiscount);

        // 4. 构建业务快照 JSON
        String snapshot = JSONUtil.createObj()
                .set("planCode", vipPlan.getPlanCode())
                .set("planName", vipPlan.getPlanName())
                .set("durationMonths", vipPlan.getDurationMonths())
                .set("pointsPrice", price)
                .toString();

        // 5. 构建订单实体
        Order order = new Order();
        order.setOrderNo(OrderNoUtil.generateVipOrder());
        order.setUserId(userId);
        order.setBizType(BizType.VIP);
        order.setBizId(planId);
        order.setBizSnapshot(snapshot);
        order.setPointsCost(price);
        order.setCouponId(bestCoupon != null ? bestCoupon.getCouponId() : null);
        order.setCouponDiscount(couponDiscount);
        order.setActualPoints(actualPoints);
        order.setStatus(OrderStatus.PENDING);
        order.setExpireTime(LocalDateTime.now().plusMinutes(15));
        orderService.save(order);

        // 6. 构建响应：先拷贝同名字段，再手动设置 orderId 和 couponName
        CreateOrderVO vo = BeanUtil.copyProperties(order, CreateOrderVO.class);
        vo.setOrderId(order.getId());
        vo.setCouponName(bestCoupon != null ? bestCoupon.getCouponName() : null);

        // 7. 发送延迟消息到超时队列（15分钟后未支付自动取消）
        try {
            rabbitTemplate.convertAndSend(VIP_ORDER_EXCHANGE, ORDER_DELAY_KEY, order.getId().toString());
        } catch (Exception e) {
            log.warn("发送订单超时延迟消息失败，等待定时补偿: orderId={}", order.getId());
        }
        //缓存用户的积分，用于快速积分校验
        String pointKey = getOrderPointsSnapshotKey(order.getId());
        Long points = pointAPI.getPointInfo(userId).getAvailablePoints();
        redisTemplate.opsForValue().set(pointKey, points.toString());
        redisTemplate.expire(pointKey, 15, TimeUnit.MINUTES);
        return vo;
    }

    /**
     * 修改订单优惠券（仅待确认状态可修改）
     */
    public CreateOrderVO updateOrderCoupon(Long orderId, UpdateOrderCouponDTO dto) {
        Long userId = UserContextHolder.getUserId();

        // 1. 校验订单存在、归属、状态
        Order order = orderService.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 计算新的优惠金额
        Integer couponDiscount = 0;
        String couponName = null;
        if (dto.getCouponId() != null) {
            BestCouponVO bestCoupon = couponAPI.getTheBestCoupon(order.getPointsCost(), userId);
            if (bestCoupon != null && bestCoupon.getCouponId().equals(dto.getCouponId())) {
                couponDiscount = bestCoupon.getCouponDiscount().intValue();
                couponName = bestCoupon.getCouponName();
            }
        }
        int actualPoints = Math.max(0, order.getPointsCost() - couponDiscount);

        // 3. 更新订单
        order.setCouponId(dto.getCouponId());
        order.setCouponDiscount(couponDiscount);
        order.setActualPoints(actualPoints);
        orderService.updateById(order);

        // 4. 构建响应：先拷贝同名字段，再手动设置 orderId 和 couponName
        CreateOrderVO vo = BeanUtil.copyProperties(order, CreateOrderVO.class);
        vo.setOrderId(order.getId());
        vo.setCouponName(couponName);
        return vo;
    }

    /**
     * 确认下单（TCC + Saga 混合模式）
     * - 积分/优惠券：TCC 模式，Try 冻结 → Confirm 提交/Cancel 释放
     * - VIP激活：Saga 模式，Try成功后直接执行，失败时Cancel释放冻结资源
     */
    public ConfirmOrderVO confirmOrder(Long orderId) {
        Long userId = UserContextHolder.getUserId();
        String key = getConfirmOrderLockKey(orderId);
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        boolean isVip = false;
        String tccXid = xid(orderId);
        Order order = orderService.getById(orderId);
        try {
            // 幂等校验 + 积分快速过滤
            valid(order, userId);
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(BizCodeEnum.ORDER_REPEAT);
            }
            // ========== [TCC Try] 冻结积分 ==========
            tccManager.tryBranch(tccXid, POINT_FREEZE,
                    () -> {
                        if (!pointAPI.freezePoints(userId, order.getActualPoints())) {
                            throw new BizException(BizCodeEnum.POINT_NOT_ENOUGH);
                        }
                    },
                    () -> pointAPI.cancelDeductPoints(userId, order.getActualPoints())
            );

            // ========== [TCC Try] 冻结优惠券 ==========
            if (order.getCouponId() != null) {
                tccManager.tryBranch(tccXid, COUPON_FREEZE,
                        () -> {
                            if (!couponAPI.freezeCoupon(order.getCouponId(), userId)) {
                                throw new BizException(BizCodeEnum.COUPON_STATUS_ERROR);
                            }
                        },
                        () -> {
                            // 优惠券冻结失败，回滚积分
                            tccManager.cancelBranch(tccXid, POINT_FREEZE,
                                    () -> pointAPI.cancelDeductPoints(userId, order.getActualPoints()));
                        }
                );
            }

            // Try 全部成功，订单状态 -> FROZEN（表示资源已冻结，等待 Confirm）
            order.setStatus(FROZEN);
            orderService.updateById(order);
            // [Saga] 激活VIP会员（非预留资源，直接执行，失败直接cancel）
            VipPlan plan = planService.getById(order.getBizId());
            if (plan != null) {
                membershipBizService.activateVip(userId, plan.getDurationMonths(), order.getId());
            }
            isVip = true;
            // ========== Confirm 阶段：TCC提交积分/优惠券（VIP已在上方Saga执行） ==========
            executeConfirm(order, userId, tccXid);

            // 返回当前订单实际状态：
            // - COMPLETED：VIP激活成功 + 积分/优惠券全部确认完毕
            // - FROZEN：VIP已激活，积分/优惠券通过MQ异步重试中（用户已可享受VIP权益）
            Order latest = orderService.getById(orderId);
            ConfirmOrderVO vo = new ConfirmOrderVO();
            vo.setOrderId(orderId);
            vo.setStatus(latest.getStatus());
            vo.setActualPoints(latest.getActualPoints());
            vo.setUpdatedAt(LocalDateTime.now());
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("确认订单异常: orderId={}", orderId, e);
            // VIP开通失败 → Saga补偿VIP + TCC Cancel释放冻结的积分/优惠券
            if(!isVip){
                cancelFrozenResources(order, tccXid);
                order.setStatus(OrderStatus.CLOSED);
                orderService.updateById(order);
                throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
            }
            throw new BizException(VIP_ERROR);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Confirm 阶段：TCC 提交积分/优惠券（VIP 激活已在主流程中完成）
     * - 积分/优惠券为 TCC 风格：提交冻结资源（冻结→已扣减/已核销），失败走MQ重试（资源已冻结，不应Cancel）
     */
    private void executeConfirm(Order order, Long userId, String tccXid) {
        boolean deductPoint = false;
        boolean useCoupon = false;
        try {
            // 1. [TCC Confirm] 确认扣减积分（冻结 → 实扣）
            pointAPI.confirmDeductPoints(userId, order.getActualPoints(),
                    "VIP_PURCHASE", order.getId(), "VIP会员购买");
            deductPoint = true;

            // 2. [TCC Confirm] 核销优惠券（冻结 → 已核销）
            if (order.getCouponId() != null) {
                couponAPI.useCoupon(userId, order.getCouponId(), order.getId());
                useCoupon = true;
            }

            // 3. [TCC] 更新事务分支状态为已确认
            tccManager.confirmBranch(tccXid, POINT_FREEZE, () -> {
            });
            if (order.getCouponId() != null) {
                tccManager.confirmBranch(tccXid, COUPON_FREEZE, () -> {
                });
            }

            // 4. 更新订单状态为已完成
            order.setStatus(OrderStatus.COMPLETED);
            orderService.updateById(order);
            log.info("订单确认完成: orderId={}, userId={}", order.getId(), userId);
        } catch (Exception e) {
            sendTccConfirmRetryMsg(order.getId());
            log.error("Confirm失败, orderId={}, deductPoint={}, useCoupon={}",
                    order.getId(), deductPoint, useCoupon, e);
        }
    }

    /**
     * TCC Confirm 重试（MQ消费者调用）：仅重试积分扣减和优惠券核销，不涉及 VIP 激活
     * 前提：VIP已开通成功，积分/优惠券已在 Try 阶段冻结
     *
     * @return true=全部确认成功，false=有确认失败需继续重试
     */
    public boolean handleTccConfirmRetry(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            log.warn("TCC Confirm重试：订单不存在, orderId={}", orderId);
            return true; // 订单不存在，消息可直接ACK
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("TCC Confirm重试：订单已完成，跳过, orderId={}", orderId);
            return true;
        }
        if (order.getStatus() != OrderStatus.FROZEN) {
            log.warn("TCC Confirm重试：订单状态非FROZEN，跳过, orderId={}, status={}", orderId, order.getStatus());
            return true;
        }

        Long userId = order.getUserId();
        String tccXid = xid(orderId);
        try {
            // 1. 确认扣减积分（幂等：FROZEN → DEDUCTED）
            tccManager.confirmBranch(tccXid, POINT_FREEZE,
                    () -> pointAPI.confirmDeductPoints(userId, order.getActualPoints(),
                            "VIP_PURCHASE", orderId, "VIP会员购买"));

            // 2. 核销优惠券（幂等：FROZEN → USED）
            if (order.getCouponId() != null) {
                tccManager.confirmBranch(tccXid, COUPON_FREEZE,
                        () -> couponAPI.useCoupon(userId, order.getCouponId(), orderId));
            }

            // 3. 全部确认成功，订单完成
            order.setStatus(OrderStatus.COMPLETED);
            orderService.updateById(order);
            log.info("TCC Confirm重试成功: orderId={}", orderId);
            return true;
        } catch (Exception e) {
            log.warn("TCC Confirm重试失败: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 发送 TCC Confirm 重试消息到 MQ（Confirm 失败时调用）
     */
    private void sendTccConfirmRetryMsg(Long orderId) {
        rabbitTemplate.convertAndSend(
                VIP_ORDER_EXCHANGE,
                TCC_CONFIRM_RETRY_KEY,
                orderId.toString()
        );
        log.info("已发送TCC Confirm重试消息: orderId={}", orderId);
    }

    /**
     * Cancel 阶段：释放冻结的积分和优惠券
     * 复用入口：OrderBizService.confirmOrder 失败时、VipMqBizService 超时取消时
     */
    public void cancelFrozenResources(Order order, String tccXid) {
        Long userId = order.getUserId();

        // 释放冻结的积分
        tccManager.cancelBranch(tccXid, POINT_FREEZE,
                () -> pointAPI.cancelDeductPoints(userId, order.getActualPoints()));

        // 释放冻结的优惠券
        if (order.getCouponId() != null) {
            tccManager.cancelBranch(tccXid, COUPON_FREEZE,
                    () -> couponAPI.releaseCoupon(order.getCouponId(), userId));
        }
    }

    //前置校验
    private void valid(Order order, Long userId) {
        if (order == null) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BizException(BizCodeEnum.ORDER_NOT_EXIST);
        }
        // 订单过期校验
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(BizCodeEnum.ORDER_EXPIRED);
        }
        // 快速判断积分
        String pointKey = getOrderPointsSnapshotKey(order.getId());
        String point = redisTemplate.opsForValue().get(pointKey);
        if (point != null && order.getActualPoints() > Long.parseLong(point)) {
            redisTemplate.delete(pointKey);
            throw new BizException(BizCodeEnum.POINT_NOT_ENOUGH);
        }
    }
}
