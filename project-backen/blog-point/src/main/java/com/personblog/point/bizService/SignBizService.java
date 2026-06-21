package com.personblog.point.bizService;

import com.personblog.common.dto.MqMessage.Point.PointMessageDTO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.point.entity.SignRecord;
import com.personblog.point.service.SignRecordService;
import com.personblog.point.vo.SignResultVO;
import com.personblog.point.vo.SignStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.personblog.common.constant.PointTypeConstants.SIGN;
import static com.personblog.common.enums.BizCodeEnum.POINT_ALREADY_SIGNED;
import static com.personblog.common.utils.DateTimeUtil.currentYearMonth;
import static com.personblog.point.config.mqConfig.PointMqConfig.POINT_EXCHANGE;
import static com.personblog.point.config.mqConfig.PointMqConfig.POINT_SIGN_KEY;
import static com.personblog.point.constant.RedisKeys.getSignKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignBizService {

    /** 基础签到积分 */
    private static final int BASE_SIGN_POINTS = 5;

    private final SignRecordService recordService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final CommonBizService commonBizService;
    /**
     * 执行每日签到
     * <p>
     * 流程：Redis Bitmap 幂等校验 → 计算连续签到 → 同步写签到记录 → MQ 异步发放积分
     */
    public SignResultVO signup() {
        Long userId = UserContextHolder.getUserId();
        LocalDate now = LocalDate.now();
        String yearMonth = currentYearMonth();
        String key = getSignKey(userId, yearMonth);
        int offset = now.getDayOfMonth() - 1;

        Boolean signed = commonBizService.trySign(key, offset);
        if(signed){
           throw new BizException(POINT_ALREADY_SIGNED);
        }
        // 计算连续签到天数（Pipeline 批量查询）
        int continuousDays = commonBizService.calculateContinuousDays(key, offset);

        // 基础积分 + 连续签到奖励
        int extraPoints = commonBizService.calculateExtraPoints(continuousDays);
        int totalPoints = BASE_SIGN_POINTS + extraPoints;

        // 同步写入签到记录，保证累计签到天数查询准确
        SignRecord record = new SignRecord();
        record.setUserId(userId);
        record.setSignDate(now);
        record.setPoints(totalPoints);
        record.setContinuousDays(continuousDays);
        recordService.save(record);

        // 本月累计签到天数（Bitmap BITCOUNT）
        Long totalSignDays = commonBizService.getTotalSignDays(key);

        // MQ 异步：更新积分余额、写积分流水、更新排行榜
        PointMessageDTO message = PointMessageDTO.builder()
                .authorId(userId)
                .points(totalPoints)
                .type(SIGN)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(POINT_EXCHANGE, POINT_SIGN_KEY, message);

        return SignResultVO.builder()
                .success(true)
                .points(totalPoints)
                .continuousDays(continuousDays)
                .totalSignDays(Math.toIntExact(totalSignDays))
                .extraPoints(extraPoints)
                .build();
    }
    /**
     * 获取签到状态
     * @return 状态返回值
     */
    public SignStatusVO getStatus() {
        Long userId = UserContextHolder.getUserId();
        String yearMonth = currentYearMonth();
        String key = getSignKey(userId, yearMonth);
        LocalDate now = LocalDate.now();
        int offset = now.getDayOfMonth() - 1;
        int continuousDays = commonBizService.calculateContinuousDays(key, offset);
        Long totalSignDays = commonBizService.getTotalSignDays(key);

        SignStatusVO vo = new SignStatusVO();
        vo.setContinuousDays(continuousDays);
        vo.setTotalSignDays(Math.toIntExact(totalSignDays));
        vo.setSigned(commonBizService.isAlreadySigned(key, offset, userId));
        vo.setSignCalendar(commonBizService.getCalender(key,offset));
        return vo;
    }
}
