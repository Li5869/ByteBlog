package com.personblog.coupon.constant;

/**
 * 优惠券模块 Redis Key 常量类
 *
 * 统一管理优惠券相关 Redis Key，避免硬编码
 * Key 命名规范：coupon:功能:标识
 *
 * @author LSH
 */
public class RedisKey {

    // =============================================
    // 优惠券库存（Lua 原子扣减用）
    // =============================================

    /**
     * 优惠券库存 Key 前缀
     * 完整 Key 格式：coupon:stock:{couponTemplateId}
     * 存储内容：优惠券剩余库存数量
     * 数据类型：String（数字）
     * 过期时间：券下架后 1 小时
     * 用途：Redis Lua 脚本原子扣减，防止超卖
     */
    public static final String COUPON_STOCK = "coupon:stock:";

    // =============================================
    // 已领取用户集合（防重复领取）
    // =============================================

    /**
     * 已领取用户集合 Key 前缀
     * 完整 Key 格式：coupon:users:{couponTemplateId}
     * 存储内容：已成功领取该优惠券的用户 ID 集合
     * 数据类型：Set
     *           member: userId
     * 过期时间：券下架后 1 小时
     * 用途：Lua 脚本内判断用户是否已领取，防止并发重复领券
     */
    public static final String COUPON_USERS = "coupon:users:";

    // =============================================
    // 领券失败冷却（防重复点击刷 DB）
    // =============================================

    // =============================================
    // Key 拼装方法
    // =============================================

    /**
     * 优惠券库存 Key
     *
     * @param couponTemplateId 优惠券模板ID
     * @return 完整的 Redis Key
     */
    public static String getCouponStockKey(Long couponTemplateId) {
        return COUPON_STOCK + couponTemplateId;
    }

    /**
     * 已领取用户集合 Key
     *
     * @param couponTemplateId 优惠券模板ID
     * @return 完整的 Redis Key
     */
    public static String getCouponUsersKey(Long couponTemplateId) {
        return COUPON_USERS + couponTemplateId;
    }
}
