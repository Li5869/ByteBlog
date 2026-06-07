package com.personblog.vip.util;

import com.personblog.common.constant.OrderStatus;
import com.personblog.common.entity.Order;
import com.personblog.vip.vo.OrderVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单工具类
 * @author LSH
 */
public class OrderUtil {

    private OrderUtil() {}

    /**
     * 构建订单时间线，返回各阶段的时间节点
     * <p>根据订单状态决定时间线条目数量和内容</p>
     */
    public static List<OrderVO.TimelineItem> buildTimeline(Order order) {
        List<OrderVO.TimelineItem> items = new ArrayList<>();
        // 创建订单（始终存在）
        items.add(OrderVO.TimelineItem.builder()
                .time(order.getCreatedAt())
                .build());

        // 已完成 → 会员开通时间
        if (order.getStatus() == OrderStatus.COMPLETED) {
            items.add(OrderVO.TimelineItem.builder()
                    .time(order.getUpdatedAt())
                    .build());
        }
        // 已取消 → 取消时间
        if (order.getStatus() == OrderStatus.CANCELLED) {
            items.add(OrderVO.TimelineItem.builder()
                    .time(order.getUpdatedAt())
                    .build());
        }
        // 已关闭 → 关闭时间
        if (order.getStatus() == OrderStatus.CLOSED) {
            items.add(OrderVO.TimelineItem.builder()
                    .time(order.getUpdatedAt())
                    .build());
        }
        return items;
    }
}
