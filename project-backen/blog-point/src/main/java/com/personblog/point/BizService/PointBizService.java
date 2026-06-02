package com.personblog.point.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.utils.DateTimeUtil;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.personblog.point.constant.RedisKeys.getPointRankIncrKey;
import static com.personblog.point.constant.RedisKeys.getPointRankKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointBizService {

    private static final String INCR_AVAILABLE = "available_points = available_points + %d";
    private static final String INCR_TOTAL = "total_points = total_points + %d";

    private final UserPointService userPointService;
    private final PointLogService pointLogService;
    private final StringRedisTemplate redisTemplate;
    private final UseApi useApi;
    private final CommonBizService commonBizService;

    /**
     * 添加或减少用户积分
     * 1. 更新用户积分余额（不存在则初始化）
     * 2. 写积分流水记录
     * 3. 积分增量缓存到 Redis Hash（定时任务批量更新排行榜）
     *
     * @param userId 用户ID
     * @param points 积分变动值（正数增加，负数减少）
     * @param type   积分类型
     * @param bizId  业务ID（可为null）
     * @param description 描述信息（可为null）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePoints(Long userId, Integer points, String type, Long bizId, String description) {
        // 1. 更新积分余额
        UserPoint exist = userPointService.getOne(
                new LambdaQueryWrapper<UserPoint>().eq(UserPoint::getUserId, userId)
        );
        if (exist == null) {
            UserPoint userPoint = new UserPoint();
            userPoint.setUserId(userId);
            userPoint.setTotalPoints(points.longValue());
            userPoint.setAvailablePoints(points.longValue());
            userPoint.setFrozenPoints(0L);
            userPointService.save(userPoint);
        } else {
            userPointService.lambdaUpdate()
                    .eq(UserPoint::getUserId, userId)
                    .setSql(String.format(INCR_AVAILABLE, points))
                    .setSql(String.format(INCR_TOTAL, points))
                    .update();
        }

        // 2. 写积分流水
        PointLog pointLog = new PointLog();
        pointLog.setUserId(userId);
        pointLog.setPoints(points);
        pointLog.setType(type);
        pointLog.setBizId(bizId);
        pointLog.setDescription(description);
        pointLog.setCreatedAt(LocalDateTime.now());
        pointLogService.save(pointLog);

        // 3. 积分增量缓存到 Redis Hash（定时任务批量更新排行榜）
        String incrKey = getPointRankIncrKey(DateTimeUtil.currentYearMonth());
        redisTemplate.opsForHash().increment(incrKey, userId.toString(), points.longValue());
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
        String yearMonth = DateTimeUtil.currentYearMonth();
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
