package com.personblog.vip.tcc;

import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.service.ITccTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TCC 事务管理器
 * 封装 TCC 生命周期管理，简化业务代码
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TccTransactionManager {

    private final ITccTransactionService tccTransactionService;

    /**
     * Try 阶段：注册分支 + 执行冻结操作，失败自动回滚
     *
     * @param xid          全局事务ID
     * @param bizType      业务类型（如 POINT_FREEZE）
     * @param freezeAction 冻结操作（如冻结积分）
     * @param rollbackAction 回滚操作（如释放冻结积分）
     */
    public void tryBranch(String xid, String bizType,
                          Runnable freezeAction, Runnable rollbackAction) {
        // 注册 TCC 分支
        if (!tccTransactionService.registerBranch(xid, bizType)) {
            throw new BizException(BizCodeEnum.ORDER_REPEAT);
        }
        try {
            freezeAction.run();
            log.info("TCC Try成功: xid={}, bizType={}", xid, bizType);
        } catch (Exception e) {
            // 冻结失败，回滚 TCC 分支 + 执行回滚操作
            tccTransactionService.cancelBranch(xid, bizType);
            rollbackAction.run();
            log.info("TCC Try失败，已回滚: xid={}, bizType={}", xid, bizType);
            throw e;
        }
    }

    /**
     * Confirm 阶段：执行业务操作 + 确认分支
     * 注意：此方法不自动补偿，需要业务层自行处理补偿逻辑
     *
     * @param xid          全局事务ID
     * @param bizType      业务类型
     * @param confirmAction 确认操作（如实扣积分）
     */
    public void confirmBranch(String xid, String bizType, Runnable confirmAction) {
        confirmAction.run();
        tccTransactionService.confirmBranch(xid, bizType);
        log.info("TCC Confirm成功: xid={}, bizType={}", xid, bizType);
    }

    /**
     * Cancel 阶段：取消分支 + 执行回滚操作
     *
     * @param xid          全局事务ID
     * @param bizType      业务类型
     * @param rollbackAction 回滚操作
     */
    public void cancelBranch(String xid, String bizType, Runnable rollbackAction) {
        if (tccTransactionService.cancelBranch(xid, bizType)) {
            try {
                rollbackAction.run();
                log.info("TCC Cancel成功: xid={}, bizType={}", xid, bizType);
            } catch (Exception e) {
                log.error("TCC Cancel异常，需人工处理: xid={}, bizType={}", xid, bizType, e);
            }
        }
    }
}