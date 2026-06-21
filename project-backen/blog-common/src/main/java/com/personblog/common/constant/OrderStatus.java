package com.personblog.common.constant;

/**
 * 订单状态常量（通用订单表 tb_order.status 字段）
 * @author LSH
 */
public class OrderStatus {

    private OrderStatus() {}

    /** 待确认：用户点击购买，未冻结积分 */
    public static final short PENDING = 0;

    /** 已冻结：TCC Try 成功，积分已冻结 */
    public static final short FROZEN = 1;

    /** 已完成：TCC Confirm 成功，积分扣减 + 会员开通 */
    public static final short COMPLETED = 2;

    /** 已取消：用户主动取消或 TCC Cancel */
    public static final short CANCELLED = 3;

    /** 已关闭：超时未操作 */
    public static final short CLOSED = 4;
}
