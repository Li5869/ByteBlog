package com.personblog.point.apiimpl;

import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.utils.DateTimeUtil;
import com.personblog.point.bizService.PointBizService;
import com.personblog.point.constant.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 积分系统 API 实现类
 *
 * @author LSH
 * @since 2026-06-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointApiImpl implements PointAPI {

    private final StringRedisTemplate redisTemplate;
    private final PointBizService pointBizService;
    /**
     * 刷新月度排行榜
     * 从 Redis Hash 读取积分增量，批量更新到排行榜 ZSet，然后删除增量缓存
     */
    @Override
    public void refreshMonthRank() {
        String yearMonth = DateTimeUtil.currentYearMonth();
        String incrKey = RedisKeys.getPointRankIncrKey(yearMonth);
        String rankKey = RedisKeys.getPointRankKey(yearMonth);

        // 1. 读取增量数据
        Map<Object, Object> increments = redisTemplate.opsForHash().entries(incrKey);
        if (increments.isEmpty()) {
            log.info("排行榜增量缓存为空，无需刷新");
            return;
        }

        log.info("开始刷新排行榜，增量用户数: {}", increments.size());

        // 2. 批量更新排行榜 ZSet
        for (Map.Entry<Object, Object> entry : increments.entrySet()) {
            String userId = (String) entry.getKey();
            long delta = Long.parseLong(entry.getValue().toString());
            redisTemplate.opsForZSet().incrementScore(rankKey, userId, delta);
        }

        // 3. 删除增量缓存
        redisTemplate.delete(incrKey);

        log.info("排行榜刷新完成，已处理 {} 个用户的积分增量", increments.size());
    }

    @Override
    public void changePoint(Long userId, Integer points, String type, Long bizId, String description) {
        pointBizService.changePoints(userId, points, type, bizId, description);
    }
}
