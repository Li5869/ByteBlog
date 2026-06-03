package com.personblog.point.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.common.utils.DateTimeUtil;
import com.personblog.point.entity.PointLog;
import com.personblog.point.entity.SignRecord;
import com.personblog.point.service.PointLogService;
import com.personblog.point.service.SignRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.personblog.point.constant.CommonConstant.AWARDED_CACHE_TTL;
import static com.personblog.point.constant.RedisKeys.getPointAwardedKey;
import static com.personblog.point.constant.RedisKeys.getPointRankKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonBizService {
    private final StringRedisTemplate redisTemplate;
    private final SignRecordService signRecordService;
    private final PointLogService pointLogService;
    /**
     * 使用 Redis Pipeline 批量查询 Bitmap，从今天往前计算连续签到天数
     */
    public int calculateContinuousDays(String key, int currentOffset) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (int i = currentOffset; i >= 0; i--) {
                src.getBit(key, i);
            }
            return null;
        });

        int continuousDays = 0;
        for (Object result : results) {
            if (Boolean.TRUE.equals(result)) {
                continuousDays++;
            } else {
                break;
            }
        }
        return continuousDays;
    }

    /**
     * 获取签到的天数数组
     */
    public List<Integer> getCalender(String key,int offset){
        List<Integer> calender = new ArrayList<>(30);
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (int i = offset; i >= 0; i--) {
                src.getBit(key, i);
            }
            return null;
        });
        int size = results.size();
        for (int i = 0; i < size; i++) {
            if (Boolean.TRUE.equals(results.get(i))) {
                calender.add(i+1);
            }
        }
        return calender;
    }
    /** 连续签到奖励阶梯：{天数, 积分}，按天数降序 */
    private static final int[][] SIGN_REWARDS = {
            {30, 150}, {14, 60}, {7, 30}, {3, 10}
    };

    /**
     * 根据连续签到天数计算额外奖励积分（取最高档）
     */
    public int calculateExtraPoints(int continuousDays) {
        for (int[] reward : SIGN_REWARDS) {
            if (continuousDays >= reward[0]) return reward[1];
        }
        return 0;
    }
    //本月连续签到
    public Long getTotalSignDays(String key) {
        return redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.stringCommands().bitCount(key.getBytes())
        );
    }
    /**
     * 签到并检查是否已签过（用于签到操作，原子性 setBit）
     */
    public Boolean trySign(String key, int offset) {
        return redisTemplate.opsForValue().setBit(key, offset, true);
    }

    /**
     * 仅查询今日是否已签到（用于状态查询，不修改数据）
     * Redis 不可用时降级查询数据库
     */
    public Boolean isAlreadySigned(String key, int offset, Long userId) {
        try {
            Boolean result = redisTemplate.opsForValue().getBit(key, offset);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Redis 查询签到状态失败，降级查询数据库: {}", e.getMessage());
            return signRecordService.exists(
                    new LambdaQueryWrapper<SignRecord>()
                            .eq(SignRecord::getUserId, userId)
                            .eq(SignRecord::getSignDate, LocalDate.now())
            );
        }
    }

    /**
     * 查询今日获得的积分总数
     */
    public Integer getTodayEarnedPoints(Long userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<PointLog> logs = pointLogService.list(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .ge(PointLog::getCreatedAt, todayStart)
                        .le(PointLog::getCreatedAt, todayEnd)
                        .gt(PointLog::getPoints, 0)
        );

        return logs.stream()
                .mapToInt(PointLog::getPoints)
                .sum();
    }

    /**
     * 获取用户当前月度排名
     */
    public Integer getUserRank(Long userId) {
        String yearMonth = DateTimeUtil.currentYearMonth();
        String rankKey = getPointRankKey(yearMonth);

        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(rankKey, userId.toString());
            return rank != null ? rank.intValue() + 1 : null;
        } catch (Exception e) {
            log.warn("获取用户排名失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAlreadyDoIt(Long bizId, Long operatorId, String type) {
        // 有 bizId 的积分类型（点赞/收藏/文章发布等），需防重复发放
            if (isPointAlreadyAwarded(operatorId, type, bizId)) {
                log.info("积分已发放过，跳过: operatorId={}, type={}, bizId={}", operatorId, type, bizId);
                return true;
            }
            return false;
    }

    /**
     * 检查积分是否已发放
     * 优先查 Redis 缓存（Set），未命中再查数据库，实现缓存 + DB 双重保障
     */
    private boolean isPointAlreadyAwarded(Long userId, String type, Long bizId) {
        String awardedKey = getPointAwardedKey(type, bizId);
        // 1. 查 Redis 缓存
        Boolean isMember = redisTemplate.opsForSet().isMember(awardedKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            return true;
        }
        // 2. 缓存未命中，查数据库
        boolean existsInDb = pointLogService.exists(
                new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .eq(PointLog::getType, type)
                        .eq(PointLog::getBizId, bizId)
        );
        // 3. DB 中存在但缓存中没有，回填缓存
        if (existsInDb) {
            redisTemplate.opsForSet().add(awardedKey, userId.toString());
            redisTemplate.expire(awardedKey, AWARDED_CACHE_TTL);
        }
        return existsInDb;
    }
    /**
     * 标记积分已发放，写入 Redis Set 缓存
     */
    public void markPointAwarded(Long userId, String type, Long bizId) {
        String awardedKey = getPointAwardedKey(type, bizId);
        redisTemplate.opsForSet().add(awardedKey, userId.toString());
        redisTemplate.expire(awardedKey, AWARDED_CACHE_TTL);
    }
}
