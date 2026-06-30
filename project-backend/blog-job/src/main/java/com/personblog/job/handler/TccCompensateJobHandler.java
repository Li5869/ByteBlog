package com.personblog.job.handler;

import com.personblog.api.vipTccAPI.VipTccAPI;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TCC 事务补偿定时任务
 * 每分钟扫描 TRYING 状态超时的分支事务，释放冻结资源
 *
 * @author LSH
 * @since 2026-06-08
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TccCompensateJobHandler {

    private final VipTccAPI vipTccAPI;

    /**
     * 补偿超时的 TCC 事务
     * 建议执行周期：每分钟一次
     */
    @XxlJob("tccCompensateJob")
    public void execute() {
        log.info("开始执行 TCC 事务补偿任务");
        vipTccAPI.compensateTimeoutTransactions();
        log.info("TCC 事务补偿任务完成");
    }
}
