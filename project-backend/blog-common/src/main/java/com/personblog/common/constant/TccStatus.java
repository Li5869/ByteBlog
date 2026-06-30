package com.personblog.common.constant;

/**
 * TCC 分布式事务状态常量
 *
 * @author LSH
 * @since 2026-06-08
 */
public class TccStatus {

    private TccStatus() {}

    /** 尝试中：Try 阶段已执行 */
    public static final String TRYING = "TRYING";

    /** 已确认：Confirm 阶段已执行 */
    public static final String CONFIRMED = "CONFIRMED";

    /** 已取消：Cancel 阶段已执行 */
    public static final String CANCELLED = "CANCELLED";
}
