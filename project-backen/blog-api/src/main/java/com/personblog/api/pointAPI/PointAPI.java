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
     * 预扣减积分（扣减可用 + 增加冻结）
     * @return true=成功，false=积分不足
     */
    boolean freezePoints(Long userId, Integer points);

    /**
     * 确认扣减（扣减冻结积分）
     */
    void confirmDeductPoints(Long userId, Integer points, String type, Long bizId, String description);

    /**
     * 取消扣减（恢复可用积分 + 扣减冻结积分）
     */
    void cancelDeductPoints(Long userId, Integer points);

    void refundPoints(Long userId, Integer actualPoints, String vipPurchaseCancel, Long OrderId, String Reason);
}
