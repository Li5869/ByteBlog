package com.personblog.point.constant;

public class RedisKeys {

    /**
     * 签到状态 Bitmap Key 前缀
     * 完整 Key 格式：point:sign:{userId}:{yyyyMM}
     * 存储内容：用户每月签到状态（Bitmap，offset 为日期-1）
     * 数据类型：String（Bitmap）
     * 过期时间：35 天
     * 用途：记录用户每日签到状态，支持连续签到统计
     */
    public static final String POINT_SIGN = "point:sign:";

    /**
     * 用户积分余额缓存前缀
     * 完整 Key 格式：point:balance:{userId}
     * 存储内容：用户积分余额信息
     * 数据类型：String（JSON）
     * 过期时间：24 小时
     */
    public static final String POINT_BALANCE = "point:balance:";

    /**
     * 月度排行榜 Key 前缀
     * 完整 Key 格式：point:rank:{yyyyMM}
     * 存储内容：月度积分排行榜
     * 数据类型：ZSet
     *           score: 积分值
     *           member: userId
     * 过期时间：45 天
     */
    public static final String POINT_RANK = "point:rank:";

    /**
     * 每日积分上限计数 Key 前缀
     * 完整 Key 格式：point:daily:{bizType}:{userId}:{yyyyMMdd}
     * 存储内容：当日已获得积分次数
     * 数据类型：String（数字）
     * 过期时间：当天 23:59:59
     * 用途：限制用户每日通过某种方式获得积分的上限
     */
    public static final String POINT_DAILY_LIMIT = "point:daily:";

    /**
     * 已发放积分用户集合 Key 前缀
     * 完整 Key 格式：point:awarded:{bizType}:{targetId}
     * 存储内容：已获得积分的用户ID集合
     * 数据类型：Set
     * 过期时间：30 天
     * 用途：防止同一目标重复发放积分（如重复点赞）
     */
    public static final String POINT_AWARDED = "point:awarded:";

    /**
     * 排行榜积分增量缓存 Key 前缀
     * 完整 Key 格式：point:rank:incr:{yyyyMM}
     * 存储内容：用户积分增量（待同步到排行榜）
     * 数据类型：Hash
     *           field: userId
     *           value: 积分增量值（可正可负）
     * 过期时间：无（定时任务处理后删除）
     * 用途：缓冲积分变动，定时任务批量更新排行榜
     */
    public static final String POINT_RANK_INCR = "point:rank:incr:";

    /**
     * 签到状态 Bitmap Key
     *
     * @param userId 用户ID
     * @param yearMonth 年月（格式：yyyyMM）
     * @return 完整的 Redis Key
     */
    public static String getPointSignKey(Long userId, String yearMonth) {
        return POINT_SIGN + userId + ":" + yearMonth;
    }

    /**
     * 用户积分余额缓存 Key
     *
     * @param userId 用户ID
     * @return 完整的 Redis Key
     */
    public static String getPointBalanceKey(Long userId) {
        return POINT_BALANCE + userId;
    }

    /**
     * 月度排行榜 Key
     *
     * @param yearMonth 年月（格式：yyyyMM）
     * @return 完整的 Redis Key
     */
    public static String getPointRankKey(String yearMonth) {
        return POINT_RANK + yearMonth;
    }

    /**
     * 每日积分上限计数 Key
     *
     * @param bizType 业务类型
     * @param userId 用户ID
     * @param date 日期（格式：yyyyMMdd）
     * @return 完整的 Redis Key
     */
    public static String getPointDailyLimitKey(String bizType, Long userId, String date) {
        return POINT_DAILY_LIMIT + bizType + ":" + userId + ":" + date;
    }

    /**
     * 已发放积分用户集合 Key
     *
     * @param bizType 业务类型
     * @param targetId 目标ID
     * @return 完整的 Redis Key
     */
    public static String getPointAwardedKey(String bizType, Long targetId) {
        return POINT_AWARDED + bizType + ":" + targetId;
    }

    /**
     * 排行榜积分增量缓存 Key
     *
     * @param yearMonth 年月（格式：yyyyMM）
     * @return 完整的 Redis Key
     */
    public static String getPointRankIncrKey(String yearMonth) {
        return POINT_RANK_INCR + yearMonth;
    }
}
