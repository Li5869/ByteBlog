package com.personblog.point.constant;

import java.time.Duration;

public class CommonConstant {
    /** 积分发放记录缓存 TTL：1 小时（过期后由 DB 查询兜底） */
    public static final Duration AWARDED_CACHE_TTL = Duration.ofHours(1);
}
