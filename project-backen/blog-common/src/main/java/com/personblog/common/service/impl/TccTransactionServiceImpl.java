package com.personblog.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.common.constant.TccStatus;
import com.personblog.common.entity.TccTransaction;
import com.personblog.common.mapper.TccTransactionMapper;
import com.personblog.common.service.ITccTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TCC 分布式事务记录表 Service 实现
 * 实现 TCC 核心逻辑：幂等性、空回滚、悬挂预防、定时补偿
 *
 * @author LSH
 * @since 2026-06-08
 */
@Slf4j
@Service
public class TccTransactionServiceImpl extends ServiceImpl<TccTransactionMapper, TccTransaction>
        implements ITccTransactionService {

    /** 补偿扫描阈值：超过 5 分钟未完成的事务 */
    private static final int COMPENSATE_TIMEOUT_MINUTES = 5;

    /** 最大重试次数 */
    private static final int DEFAULT_MAX_RETRY = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerBranch(String xid, String bizType) {
        // 检查是否已存在（防止悬挂：Cancel 后 Try 再执行）
        TccTransaction existing = getByXidAndBizType(xid, bizType);
        if (existing != null) {
            // 已经 Cancel 过，不能再 Try（悬挂预防）
            if (TccStatus.CANCELLED.equals(existing.getStatus())) {
                log.warn("TCC 悬挂预防：分支已取消, xid={}, bizType={}", xid, bizType);
                return false;
            }
            // 已经存在，幂等返回
            log.info("TCC 幂等：分支已存在, xid={}, bizType={}, status={}", xid, bizType, existing.getStatus());
            return true;
        }

        // 首次注册
        TccTransaction tx = TccTransaction.builder()
                .xid(xid)
                .bizType(bizType)
                .status(TccStatus.TRYING)
                .retryCount(0)
                .maxRetry(DEFAULT_MAX_RETRY)
                .build();
        this.save(tx);
        log.info("TCC 注册分支成功: xid={}, bizType={}", xid, bizType);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmBranch(String xid, String bizType) {
        TccTransaction tx = getByXidAndBizType(xid, bizType);
        if (tx == null) {
            log.warn("TCC Confirm：分支不存在, xid={}, bizType={}", xid, bizType);
            return false;
        }
        if (TccStatus.CONFIRMED.equals(tx.getStatus())) {
            log.info("TCC 幂等：分支已确认, xid={}, bizType={}", xid, bizType);
            return false;
        }
        if (TccStatus.CANCELLED.equals(tx.getStatus())) {
            log.warn("TCC Confirm：分支已取消, xid={}, bizType={}", xid, bizType);
            return false;
        }

        // 更新状态为 CONFIRMED
        tx.setStatus(TccStatus.CONFIRMED);
        tx.setUpdatedAt(LocalDateTime.now());
        this.updateById(tx);
        log.info("TCC 确认分支成功: xid={}, bizType={}", xid, bizType);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelBranch(String xid, String bizType) {
        TccTransaction tx = getByXidAndBizType(xid, bizType);
        if (tx == null) {
            // 分支不存在，Try 未执行，空回滚（记录一条 CANCELLED 记录防悬挂）
            log.info("TCC 空回滚：分支不存在, xid={}, bizType={}", xid, bizType);
            TccTransaction cancelTx = TccTransaction.builder()
                    .xid(xid)
                    .bizType(bizType)
                    .status(TccStatus.CANCELLED)
                    .retryCount(0)
                    .maxRetry(DEFAULT_MAX_RETRY)
                    .build();
            this.save(cancelTx);
            return false;
        }
        if (TccStatus.CANCELLED.equals(tx.getStatus())) {
            log.info("TCC 幂等：分支已取消, xid={}, bizType={}", xid, bizType);
            return false;
        }
        if (TccStatus.CONFIRMED.equals(tx.getStatus())) {
            log.warn("TCC Cancel：分支已确认, xid={}, bizType={}", xid, bizType);
            return false;
        }

        // 更新状态为 CANCELLED
        tx.setStatus(TccStatus.CANCELLED);
        tx.setUpdatedAt(LocalDateTime.now());
        this.updateById(tx);
        log.info("TCC 取消分支成功: xid={}, bizType={}", xid, bizType);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutTransactions() {
        // 仅标记状态为 CANCELLED，业务回滚由协调者执行
        List<TccTransaction> timeoutList = findTimeoutTransactions(COMPENSATE_TIMEOUT_MINUTES);
        for (TccTransaction tx : timeoutList) {
            try {
                this.cancelBranch(tx.getXid(), tx.getBizType());
            } catch (Exception e) {
                log.error("TCC 补偿标记失败: xid={}, bizType={}", tx.getXid(), tx.getBizType(), e);
            }
        }
    }

    @Override
    public List<TccTransaction> findTimeoutTransactions(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return this.lambdaQuery()
                .eq(TccTransaction::getStatus, TccStatus.TRYING)
                .lt(TccTransaction::getCreatedAt, threshold)
                .orderByAsc(TccTransaction::getCreatedAt)
                .last("LIMIT 100")
                .list();
    }

    @Override
    public List<TccTransaction> findByXid(String xid) {
        return this.lambdaQuery()
                .eq(TccTransaction::getXid, xid)
                .list();
    }

    /**
     * 根据 xid 和 bizType 查询事务记录（唯一约束）
     */
    private TccTransaction getByXidAndBizType(String xid, String bizType) {
        return this.lambdaQuery()
                .eq(TccTransaction::getXid, xid)
                .eq(TccTransaction::getBizType, bizType)
                .one();
    }
}
