package com.personblog.common.constant;

/**
 * 订单状态常量（通用订单表 tb_order.status 字段）
 * @author LSH
 */
public class OrderStatus {

    private OrderStatus() {}

    /** 待确认：用户点击购买，待扣减积分 */
    public static final short PENDING = 0;

    /** 已完成：积分扣减 + 优惠券核销 + VIP激活 全部完成 */
    public static final short COMPLETED = 2;

    /** 已取消：用户主动取消 */
    public static final short CANCELLED = 3;

    /** 已关闭：超时未操作 */
    public static final short CLOSED = 4;
}
