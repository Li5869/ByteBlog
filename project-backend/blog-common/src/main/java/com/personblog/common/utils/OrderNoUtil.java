package com.personblog.common.utils;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

/**
 * 订单号生成工具类
 * 基于雪花算法（MyBatis Plus IdWorker），保证分布式环境下的唯一性
 *
 * 格式：前缀 + 雪花ID（如 VIP1929837465123456789）
 *
 * @author LSH
 */
public class OrderNoUtil {

    private OrderNoUtil() {
    }

    /**
     * 生成带前缀的订单号
     *
     * @param prefix 业务前缀（如 VIP、ORDER）
     * @return 订单号，如 VIP1929837465123456789
     */
    public static String generate(String prefix) {
        return prefix + IdWorker.getIdStr();
    }

    /**
     * 生成 VIP 订单号
     *
     * @return 订单号，如 VIP1929837465123456789
     */
    public static String generateVipOrder() {
        return generate("VIP");
    }

    /**
     * 生成通用订单号
     *
     * @return 订单号，如 ORD1929837465123456789
     */
    public static String generateOrder() {
        return generate("ORD");
    }
}
