package com.personblog.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.entity.TccTransaction;

import java.util.List;

/**
 * TCC 分布式事务记录表 Service 接口
 *
 * @author LSH
 * @since 2026-06-08
 */
public interface ITccTransactionService extends IService<TccTransaction> {

    /**
     * 注册分支事务（Try 阶段调用）
     *
     * @param xid     全局事务ID
     * @param bizType 业务类型（同一 xid 下唯一，用作幂等键）
     * @return true=首次注册成功，false=已存在（幂等）或已取消（悬挂预防）
     */
    boolean registerBranch(String xid, String bizType);

    /**
     * 确认分支事务（Confirm 阶段调用）
     *
     * @param xid     全局事务ID
     * @param bizType 业务类型
     */
    boolean confirmBranch(String xid, String bizType);

    /**
     * 取消分支事务（Cancel 阶段调用）
     *
     * @param xid     全局事务ID
     * @param bizType 业务类型
     */
    boolean cancelBranch(String xid, String bizType);

    /**
     * 补偿超时未完成的事务（定时任务调用）
     * 扫描 TRYING 状态超过阈值时间的记录，执行 Cancel
     */
    void compensateTimeoutTransactions();

    /**
     * 查询超时的 TRYING 记录（补偿任务用）
     *
     * @param timeoutMinutes 超时阈值（分钟）
     * @return 超时的事务记录列表
     */
    List<TccTransaction> findTimeoutTransactions(int timeoutMinutes);

    /**
     * 根据 xid 查询该全局事务下的所有分支记录
     */
    List<TccTransaction> findByXid(String xid);
}
