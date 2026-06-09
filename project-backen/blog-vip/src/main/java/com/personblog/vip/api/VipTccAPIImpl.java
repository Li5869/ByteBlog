package com.personblog.vip.api;

import com.personblog.api.vipTccAPI.VipTccAPI;
import com.personblog.common.constant.OrderStatus;
import com.personblog.common.entity.Order;
import com.personblog.common.entity.TccTransaction;
import com.personblog.common.service.IOrderService;
import com.personblog.common.service.ITccTransactionService;
import com.personblog.vip.bizService.OrderBizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * VIP TCC 补偿 API 实现
 *
 * @author LSH
 * @since 2026-06-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipTccAPIImpl implements VipTccAPI {

    /** 补偿扫描阈值（分钟） */
    private static final int TIMEOUT_MINUTES = 5;

    private final ITccTransactionService tccTransactionService;
    private final IOrderService orderService;
    private final OrderBizService orderBizService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutTransactions() {
        // 1. 查询超时的 TRYING 记录
        List<TccTransaction> timeoutList = tccTransactionService.findTimeoutTransactions(TIMEOUT_MINUTES);
        if (timeoutList.isEmpty()) {
            return;
        }

        log.info("TCC 补偿：扫描到超时事务数量={}", timeoutList.size());

        int successCount = 0;
        int failCount = 0;

        for (TccTransaction tx : timeoutList) {
            try {
                // 2. 从 xid 解析订单ID（格式：ORDER_{orderId}）
                Long orderId = parseOrderId(tx.getXid());
                if (orderId == null) {
                    log.warn("TCC 补偿：无法解析订单ID, xid={}", tx.getXid());
                    continue;
                }

                // 3. 查询订单
                Order order = orderService.getById(orderId);
                if (order == null) {
                    log.warn("TCC 补偿：订单不存在, xid={}, orderId={}", tx.getXid(), orderId);
                    // 订单已删除，标记分支为已取消
                    tccTransactionService.cancelBranch(tx.getXid(), tx.getBizType());
                    continue;
                }

                // 4. 非 FROZEN 状态的跳过（已处理过）
                if (order.getStatus() != OrderStatus.FROZEN) {
                    log.info("TCC 补偿：订单非冻结状态，跳过, xid={}, status={}", tx.getXid(), order.getStatus());
                    continue;
                }

                // 5. 释放冻结资源 + 关闭订单
                orderBizService.cancelFrozenResources(order, tx.getXid());
                order.setStatus(OrderStatus.CLOSED);
                orderService.updateById(order);

                successCount++;
                log.info("TCC 补偿成功：订单已关闭, xid={}, orderId={}", tx.getXid(), orderId);
            } catch (Exception e) {
                failCount++;
                log.error("TCC 补偿失败, xid={}, bizType={}", tx.getXid(), tx.getBizType(), e);
            }
        }

        log.info("TCC 补偿完成：成功={}, 失败={}", successCount, failCount);
    }

    /**
     * 从 xid 解析订单ID
     * @param xid 格式：ORDER_{orderId}
     */
    private Long parseOrderId(String xid) {
        if (xid == null || !xid.startsWith("ORDER_")) {
            return null;
        }
        try {
            return Long.parseLong(xid.substring("ORDER_".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
