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
import com.personblog.common.dto.MqMessage.Vip.OrderConfirmMessageDTO;
import com.personblog.common.entity.LocalMessage;
import com.personblog.common.entity.Order;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.service.IOrderService;
import com.personblog.common.service.LocalMessageService;
import com.personblog.common.utils.OrderNoUtil;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.vip.dto.CreateOrderDTO;
import com.personblog.vip.dto.OrderQueryDTO;
import com.personblog.vip.dto.UpdateOrderCouponDTO;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.service.IVipPlanService;
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
import static com.personblog.vip.config.mqConfig.VipOrderMqConfig.*;
import static com.personblog.vip.constant.RedisKeys.*;


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
    private final LocalMessageService localMessageService;
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
        //缓存用户的积分
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
     * 确认下单（TCC 模式：Try 冻结资源 → 同步 Confirm 提交 → 失败自动 Cancel 回滚）
     * <p>
     * 同步 Confirm 保证用户立即感知结果；本地消息表兜底进程崩溃场景。
     */
    public ConfirmOrderVO confirmOrder(Long orderId) {
        Long userId = UserContextHolder.getUserId();
        String key = getConfirmOrderLockKey(orderId);
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(BizCodeEnum.ORDER_REPEAT);
            }

            Order order = orderService.getById(orderId);
            // 幂等校验 + 积分快速过滤
            valid(order, userId);

            // ========== Try 阶段：冻结积分 ==========
            boolean pointFreeze = pointAPI.freezePoints(userId, order.getActualPoints());
            if (!pointFreeze) {
                throw new BizException(BizCodeEnum.POINT_NOT_ENOUGH);
            }

            // ========== Try 阶段：冻结优惠券（失败需回滚积分） ==========
            if (order.getCouponId() != null) {
                boolean couponFreeze = couponAPI.freezeCoupon(order.getCouponId(), userId);
                if (!couponFreeze) {
                    // 内联补偿：回滚已冻结的积分
                    pointAPI.cancelDeductPoints(userId, order.getActualPoints());
                    throw new BizException(BizCodeEnum.COUPON_STATUS_ERROR);
                }
            }

            // Try 成功，订单状态 -> FROZEN
            order.setStatus(FROZEN);
            orderService.updateById(order);

            // 写本地消息表（持久化，用于进程崩溃时 MQ 兜底重试）
            OrderConfirmMessageDTO msgDTO = OrderConfirmMessageDTO.builder()
                    .orderId(orderId)
                    .orderNo(order.getOrderNo())
                    .userId(userId)
                    .build();
            LocalMessage localMessage = LocalMessage.builder()
                    .bizId(orderId.toString())
                    .bizUserId(userId.toString())
                    .bizType("PAID_VIP")
                    .exchange(VIP_ORDER_EXCHANGE)
                    .routingKey(ORDER_CONFIRM_KEY)
                    .messageBody(JSONUtil.toJsonStr(msgDTO))
                    .build();
            localMessageService.save(localMessage);
            // 尝试立即发送 MQ（失败不影响主流程，定时任务会补偿）
            try {
                localMessageService.trySend(localMessage);
            } catch (Exception e) {
                log.warn("MQ 立即发送失败，等待定时补偿: orderId={}", orderId);
            }

            // ========== Confirm 阶段：同步提交（用户立即感知结果） ==========
            executeConfirm(order, userId);

            // 同步Confirm成功，标记本地消息为已完成（避免定时任务无意义重试）
            localMessage.setStatus(1);
            localMessageService.updateById(localMessage);

            // Confirm 成功，返回已完成
            ConfirmOrderVO vo = new ConfirmOrderVO();
            vo.setOrderId(orderId);
            vo.setStatus(OrderStatus.COMPLETED);
            vo.setActualPoints(order.getActualPoints());
            vo.setUpdatedAt(LocalDateTime.now());
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("确认订单异常: orderId={}", orderId, e);
            throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Confirm 阶段：实扣积分、核销优惠券、激活VIP
     * 任意一步失败则 Cancel 回滚所有已冻结资源
     */
    private void executeConfirm(Order order, Long userId) {
        try {
            // 1. 确认扣减积分（冻结 → 实扣）
            pointAPI.confirmDeductPoints(userId, order.getActualPoints(),
                    "VIP_PURCHASE", order.getId(), "VIP会员购买");

            // 2. 核销优惠券
            if (order.getCouponId() != null) {
                couponAPI.useCoupon(userId, order.getCouponId(), order.getId());
            }

            // 3. 激活VIP会员
            VipPlan plan = planService.getById(order.getBizId());
            if (plan != null) {
                membershipBizService.activateVip(userId, plan.getDurationMonths(), order.getId());
            }

            // 4. 更新订单状态为已完成
            order.setStatus(OrderStatus.COMPLETED);
            orderService.updateById(order);
            log.info("订单确认完成: orderId={}, userId={}", order.getId(), userId);
        } catch (Exception e) {
            // Confirm 任意一步失败 → Cancel 回滚已冻结资源 + 关闭订单
            log.error("同步Confirm失败，开始Cancel回滚, orderId={}", order.getId(), e);
            cancelFrozenResources(order);
            order.setStatus(OrderStatus.CLOSED);
            orderService.updateById(order);
            throw new BizException(BizCodeEnum.ORDER_STATUS_ERROR);
        }
    }

    /**
     * Cancel 阶段：释放冻结的积分和优惠券
     * 异常仅记录日志（需人工处理），防止影响主流程
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
