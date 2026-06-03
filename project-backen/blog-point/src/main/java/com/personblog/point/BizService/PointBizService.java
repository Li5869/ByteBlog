package com.personblog.point.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.point.dto.PointLogQueryDTO;
import com.personblog.point.entity.PointLog;
import com.personblog.point.entity.UserPoint;
import com.personblog.point.service.PointLogService;
import com.personblog.point.service.UserPointService;
import com.personblog.point.util.PointConvertUtil;
import com.personblog.point.vo.PointBalanceVO;
import com.personblog.point.vo.PointLogVO;
import com.personblog.point.vo.PointRankItemVO;
import com.personblog.point.vo.PointRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.common.utils.DateTimeUtil.currentYearMonth;
import static com.personblog.common.utils.DateTimeUtil.currentYearMonthDay;
import static com.personblog.point.constant.PointTypeConstants.ADMIN_ADJUST;
import static com.personblog.point.constant.RedisKeys.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointBizService {

    private static final String INCR_AVAILABLE = "available_points = available_points + %d";
    private static final String INCR_TOTAL = "total_points = total_points + %d";
    private static final int DAILY_POINT_LIMIT = 150;

    /**
     * Lua 脚本：原子性计算每日积分
     * 从 classpath:lua/point_daily_limit.lua 加载
     */
    private static final DefaultRedisScript<Long> POINT_LIMIT_SCRIPT = new DefaultRedisScript<>();
    static {
        POINT_LIMIT_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/point_daily_limit.lua")));
        POINT_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final UserPointService userPointService;
    private final PointLogService pointLogService;
    private final StringRedisTemplate redisTemplate;
    private final UseApi useApi;
    private final CommonBizService commonBizService;

    /**
     * 添加或减少用户积分
     * 1. 每日积分上限检查（正向积分且非管理员调整）
     * 2. 更新用户积分余额（不存在则初始化）
     * 3. 写积分流水记录
     * 4. 积分增量缓存到 Redis Hash（定时任务批量更新排行榜）
     *
     * @param userId      用户ID
     * @param points      积分变动值（正数增加，负数减少）
     * @param type        积分类型
     * @param bizId       业务ID（可为null）
     * @param description 描述信息（可为null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePoints(Long userId, Integer points, String type, Long bizId, String description) {
        // 正向积分且非管理员调整，需检查每日上限
        Integer effectivePoints = points;
        if (points > 0 && !ADMIN_ADJUST.equals(type)) {
            String totalKey = getPointDailyTotalKey(userId, currentYearMonthDay());
            effectivePoints = getEffectivePoints(points, totalKey);
            if (effectivePoints == 0) {
                log.info("用户 {} 今日积分已达上限，跳过", userId);
                return;
            }
        }

        // 1. 更新积分余额
        UserPoint exist = userPointService.getOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );
        if (exist == null) {
            UserPoint userPoint = new UserPoint();
            userPoint.setUserId(userId);
            userPoint.setTotalPoints((long) effectivePoints);
            userPoint.setAvailablePoints((long) effectivePoints);
            userPoint.setFrozenPoints(0L);
            userPointService.save(userPoint);
        } else {
            userPointService.lambdaUpdate()
                    .eq(UserPoint::getUserId, userId)
                    .setSql(String.format(INCR_AVAILABLE, effectivePoints))
                    .setSql(String.format(INCR_TOTAL, effectivePoints))
                    .update();
        }

        // 2. 写积分流水
        PointLog pointLog = new PointLog();
        pointLog.setUserId(userId);
        pointLog.setPoints(effectivePoints);
        pointLog.setType(type);
        pointLog.setBizId(bizId);
        pointLog.setDescription(description);
        pointLog.setCreatedAt(LocalDateTime.now());
        pointLogService.save(pointLog);

        // 3. 积分增量缓存到 Redis Hash（定时任务批量更新排行榜）
        String incrKey = getPointRankIncrKey(currentYearMonth());
        redisTemplate.opsForHash().increment(incrKey, userId.toString(), effectivePoints);
    }

    /**
     * 计算实际可发放的积分数（Lua 脚本原子操作，防止并发超发）
     *
     * @param points   本次积分变动值
     * @param totalKey 每日总积分 Redis Key
     * @return 实际可发放的积分数，0 表示已达上限
     */
    private Integer getEffectivePoints(Integer points, String totalKey) {
        try {
            // 计算到当天 23:59:59 的剩余秒数
            LocalDateTime midnight = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
            long expireSeconds = Duration.between(LocalDateTime.now(), midnight).getSeconds();

            // 执行 Lua 脚本，原子性完成：判断上限 + 递增 + 设置过期
            Long result = redisTemplate.execute(
                    POINT_LIMIT_SCRIPT,
                    Collections.singletonList(totalKey),
                    points.toString(),
                    String.valueOf(DAILY_POINT_LIMIT),
                    String.valueOf(expireSeconds)
            );

            return result != null ? result.intValue() : points;
        } catch (Exception e) {
            log.warn("Redis 每日积分检查失败，降级放行: {}", e.getMessage());
            return points;
        }
    }

    /**
     * 获取积分余额
     * 查询用户积分信息、今日获得积分、当前月度排名
     */
    public PointBalanceVO getBalance() {
        Long userId = UserContextHolder.getUserId();

        UserPoint userPoint = userPointService.getOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );

        Long totalPoints = 0L;
        Long availablePoints = 0L;
        if (userPoint != null) {
            totalPoints = userPoint.getTotalPoints();
            availablePoints = userPoint.getAvailablePoints();
        }

        Integer todayEarned = commonBizService.getTodayEarnedPoints(userId);
        Integer rank = commonBizService.getUserRank(userId);

        return PointBalanceVO.builder()
                .totalPoints(totalPoints)
                .availablePoints(availablePoints)
                .todayEarned(todayEarned)
                .rank(rank)
                .build();
    }

    /**
     * 获取积分排行榜
     * 从Redis ZSet获取月度排行榜，批量查询用户信息填充昵称和头像
     */
    public PointRankVO getRankList(Integer topN) {
        Long userId = UserContextHolder.getUserId();
        String yearMonth = currentYearMonth();
        String rankKey = getPointRankKey(yearMonth);

        // 获取排行榜前N名
        Set<ZSetOperations.TypedTuple<String>> topSet =
                redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, topN - 1);

        List<PointRankItemVO> records = new ArrayList<>();
        if (topSet != null && !topSet.isEmpty()) {
            // 批量获取用户信息
            Map<Long, UserDTO> userMap = getUserMapFromRankSet(topSet);

            // 构建排行榜记录
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : topSet) {
                Long uid = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
                Long points = Objects.requireNonNull(tuple.getScore()).longValue();

                UserDTO userDTO = userMap.get(uid);
                String nickname = userDTO != null ? userDTO.getNickname() : "用户" + uid;
                String avatar = userDTO != null ? userDTO.getAvatar() : null;

                records.add(PointRankItemVO.builder()
                        .rank(rank++)
                        .userId(uid)
                        .nickname(nickname)
                        .avatar(avatar)
                        .points(points)
                        .build());
            }
        }

        // 获取当前用户排名信息
        Long totalUsers = redisTemplate.opsForZSet().zCard(rankKey);
        if (totalUsers == null) {
            totalUsers = 0L;
        }

        Long myRankLong = redisTemplate.opsForZSet().reverseRank(rankKey, userId.toString());
        Integer myRank = myRankLong != null ? myRankLong.intValue() + 1 : null;

        Double myScore = redisTemplate.opsForZSet().score(rankKey, userId.toString());
        Long myPoints = myScore != null ? myScore.longValue() : 0L;

        return PointRankVO.builder()
                .yearMonth(yearMonth)
                .totalUsers(totalUsers.intValue())
                .myRank(myRank)
                .myPoints(myPoints)
                .records(records)
                .build();
    }

    /**
     * 获取积分流水
     * 分页查询用户积分变动记录，支持按类型筛选
     */
    public Page<PointLogVO> getPointLogs(PointLogQueryDTO queryDTO) {
        Long userId = UserContextHolder.getUserId();

        Page<PointLog> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<PointLog> wrapper = new LambdaQueryWrapper<PointLog>()
                .eq(PointLog::getUserId, userId)
                .eq(queryDTO.getType() != null && !queryDTO.getType().isEmpty(),
                        PointLog::getType, queryDTO.getType())
                .orderByDesc(PointLog::getCreatedAt);

        pointLogService.page(page, wrapper);

        // 转换为VO分页对象
        Page<PointLogVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PointLogVO> voList = page.getRecords().stream()
                .map(PointConvertUtil::toPointLogVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 从排行榜ZSet结果中批量获取用户信息
     */
    private Map<Long, UserDTO> getUserMapFromRankSet(Set<ZSetOperations.TypedTuple<String>> topSet) {
        List<Long> userIds = topSet.stream()
                .map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())))
                .collect(Collectors.toList());

        try {
            List<UserDTO> userList = useApi.getUserInfo(userIds);
            return userList.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u, (a, b) -> a));
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
