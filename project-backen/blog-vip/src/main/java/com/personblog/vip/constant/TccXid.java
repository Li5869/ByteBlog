package com.personblog.vip.constant;

/**
 * TCC 全局事务 ID 常量
 * 格式：{前缀}_{业务ID}
 * 示例：ORDER_1234567890123456789
 *
 * @author LSH
 * @since 2026-06-08
 */
public class TccXid {

    private TccXid() {}

    /** VIP 会员购买事务前缀 */
    public static final String ORDER_PREFIX = "ORDER";

    /**
     * 生成全局事务 ID
     * @param orderId 订单ID
     * @return 格式：ORDER_{orderId}
     */
    public static String xid(Long orderId) {
        return ORDER_PREFIX + "_" + orderId;
    }
}
