package com.personblog.api.vipTccAPI;

/**
 * VIP TCC 补偿 API
 * 用于定时任务跨模块调用 TCC 超时事务补偿
 *
 * @author LSH
 * @since 2026-06-08
 */
public interface VipTccAPI {

    /**
     * 补偿超时的 TCC 事务
     * 扫描 TRYING 状态超时的分支事务，释放冻结的积分和优惠券
     */
    void compensateTimeoutTransactions();
}
