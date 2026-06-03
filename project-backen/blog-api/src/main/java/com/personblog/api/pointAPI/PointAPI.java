package com.personblog.api.pointAPI;

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
}
