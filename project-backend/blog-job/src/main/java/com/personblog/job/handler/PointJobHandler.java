package com.personblog.job.handler;

import com.personblog.api.pointAPI.PointAPI;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分系统定时任务处理器
 *
 * @author LSH
 * @since 2026-06-01
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PointJobHandler {

    private final PointAPI pointAPI;

    /**
     * 刷新月度排行榜
     * 每小时执行一次，从 Redis Hash 读取积分增量，批量更新到排行榜 ZSet
     */
    @XxlJob("refreshPointRank")
    public void refreshPointRank() {
        log.info("开始执行排行榜刷新任务");
        pointAPI.refreshMonthRank();
        log.info("排行榜刷新任务完成");
    }
}
