package com.personblog.point.apiimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.api.pointAPI.vo.PointInfoVO;
import com.personblog.common.utils.DateTimeUtil;
import com.personblog.point.bizService.PointBizService;
import com.personblog.point.constant.RedisKeys;
import com.personblog.point.entity.PointLog;
import com.personblog.point.entity.UserPoint;
import com.personblog.point.service.PointLogService;
import com.personblog.point.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final UserPointService userPointService;
    private final PointLogService pointLogService;
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

    @Override
    public PointInfoVO getPointInfo(Long userId) {
        UserPoint userPoint = userPointService.getOne(new LambdaQueryWrapper<UserPoint>()
                .eq(UserPoint::getUserId, userId));
        return PointInfoVO.builder()
                .frozenPoints(userPoint.getFrozenPoints())
                .availablePoints(userPoint.getAvailablePoints())
                .build();
    }

    @Override
    public boolean freezePoints(Long userId, Integer points) {
        // 原子减
        return userPointService.lambdaUpdate()
                .eq(UserPoint::getUserId, userId)
                .ge(UserPoint::getAvailablePoints, points)
                .setSql("available_points = available_points - " + points)  // 原子减
                .setSql("frozen_points = frozen_points + " + points)
                .update();
    }

    @Override
    public void confirmDeductPoints(Long userId, Integer points, String type, Long bizId, String description) {
        // 确认扣减：减少冻结积分（积分已在预扣减时从可用积分中扣除）
        // ge 条件防御重复调用导致 frozen_points 变为负数
        userPointService.lambdaUpdate()
                .eq(UserPoint::getUserId, userId)
                .ge(UserPoint::getFrozenPoints, points)
                .setSql("frozen_points = frozen_points - " + points)
                .update();
        // 写积分流水
        PointLog pointLog = new PointLog();
        pointLog.setUserId(userId);
        pointLog.setPoints(-points);  // 负数表示扣减
        pointLog.setType(type);
        pointLog.setBizId(bizId);
        pointLog.setDescription(description);
        pointLog.setCreatedAt(LocalDateTime.now());
        pointLogService.save(pointLog);
    }

    @Override
    public void cancelDeductPoints(Long userId, Integer points) {
        // 取消扣减：恢复可用积分，减少冻结积分
        // ge 条件防御空回滚：Try 从未执行时 frozen_points=0，避免变为负数
        userPointService.lambdaUpdate()
                .eq(UserPoint::getUserId, userId)
                .ge(UserPoint::getFrozenPoints, points)
                .setSql("available_points = available_points + " + points)
                .setSql("frozen_points = frozen_points - " + points)
                .update();
    }
    //confirm失败，退回积分
    @Override
    public void refundPoints(Long userId, Integer actualPoints, String vipPurchaseCancel, Long id, String Reason) {
        boolean update = userPointService.lambdaUpdate()
                .eq(UserPoint::getUserId, userId)
                .setSql("available_points = available_points + " + actualPoints)
                .update();
        if(update){
            PointLog pointLog = new PointLog();
            pointLog.setUserId(userId);
            pointLog.setDescription(Reason);
            pointLog.setType(vipPurchaseCancel);
            pointLog.setBizId(id);
            pointLogService.save(pointLog);
        }
    }
}
