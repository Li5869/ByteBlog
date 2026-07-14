package com.personblog.vip.bizService;

import com.personblog.common.constant.OrderStatus;
import com.personblog.common.entity.Order;
import com.personblog.common.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
    private final RedissonClient redissonClient;

    /**
     * 订单超时处理：关闭待确认订单
     * 本体模式下无 FROZEN 中间态，PENDING 超时直接关闭即可（无需释放冻结资源）
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
            // 幂等：只处理待确认状态
            if (order.getStatus() != OrderStatus.PENDING) {
                log.info("超时处理：订单状态无需处理，跳过, orderId={}, status={}",
                        orderId, order.getStatus());
                return;
            }

            // PENDING 超时直接关闭（无需释放冻结资源，因为确认下单在同一本地事务中原子完成）
            order.setStatus(OrderStatus.CLOSED);
            orderService.updateById(order);
            log.info("订单超时取消成功: orderId={}", orderId);
        } catch (Exception e) {
            log.error("超时处理异常: orderId={}", orderId, e);
            throw e;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
