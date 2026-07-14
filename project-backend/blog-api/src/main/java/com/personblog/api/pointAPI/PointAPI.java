package com.personblog.api.pointAPI;

import com.personblog.api.pointAPI.vo.PointInfoVO;

/**
 * 积分系统 API 接口
 * 用于跨模块调用积分服务
 *
 * @author LSH
 * @since 2026-06-01
 */
public interface PointAPI {

    /**
     * 刷新月度排行榜
     * 从 Redis Hash 读取积分增量，批量更新到排行榜 ZSet，然后删除增量缓存
     */
    void refreshMonthRank();

    /**
     *
     */
    void changePoint(Long userId, Integer points, String type, Long bizId, String description);

    /**
     * 获取当前用户积分信息
     * @return 积分信息
     */
    PointInfoVO getPointInfo(Long userId);

       /**
     * 直接扣减积分（原子防超扣 + 写流水）
     * 单体本地事务模式：替代 freezePoints + confirmDeductPoints 两步
     *
     * @return true=扣减成功，false=积分不足
     */
    boolean deductPoints(Long userId, Integer points, String type, Long bizId, String description);
}
