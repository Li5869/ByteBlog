package com.personblog.vip.constant;

/**
 * VIP 模块 Redis Key 常量
 * @author LSH
 */
public class RedisKeys {

    private RedisKeys() {}

    /** 套餐列表缓存：vip:plan:list (Hash, 永久) */
    public static final String PLAN_LIST = "vip:plan:list";

    /** 用户会员状态缓存前缀：vip:info:{userId} (Hash, TTL与到期时间对齐) */
    public static final String VIP_INFO = "vip:info:";

    /** 防重复下单锁前缀：vip:preorder:{userId}:{planId} (String, 5秒) */
    public static final String ORDER_REPEAT = "vip:preorder:";

    /** 确认订单分布式锁前缀：vip:confirm:lock:{orderId} (Redisson, 5秒等待/30秒释放) */
    public static final String CONFIRM_ORDER_LOCK = "vip:confirm:lock:";

    /** 订单积分快照前缀：vip:order:points:{orderId} (String, 30分钟，与订单过期对齐) */
    public static final String ORDER_POINTS_SNAPSHOT = "vip:order:points:";

    /**
     * 用户会员状态缓存 Key
     */
    public static String getVipInfoKey(Long userId) {
        return VIP_INFO + userId;
    }

    /**
     * 防重复下单锁 Key
     */
    public static String getOrderRepeatKey(Long userId, Long planId) {
        return ORDER_REPEAT + userId + ":" + planId;
    }

    /**
     * 确认订单分布式锁 Key
     */
    public static String getConfirmOrderLockKey(Long orderId) {
        return CONFIRM_ORDER_LOCK + orderId;
    }

    /**
     * 订单积分快照 Key
     */
    public static String getOrderPointsSnapshotKey(Long orderId) {
        return ORDER_POINTS_SNAPSHOT + orderId;
    }
}
