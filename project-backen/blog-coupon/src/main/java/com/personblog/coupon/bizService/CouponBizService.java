package com.personblog.coupon.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.dto.MqMessage.Coupon.CouponClaimMessageDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.coupon.dto.CouponClaimDTO;
import com.personblog.coupon.dto.CouponZoneQueryDTO;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.CouponTemplateService;
import com.personblog.coupon.service.UserCouponService;
import com.personblog.coupon.vo.CouponDetailVO;
import com.personblog.coupon.vo.CouponZoneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.personblog.common.enums.BizCodeEnum.*;
import static com.personblog.coupon.config.mqConfig.CouponMqConfig.COUPON_CLAIM_KEY;
import static com.personblog.coupon.config.mqConfig.CouponMqConfig.COUPON_EXCHANGE;
import static com.personblog.coupon.constant.RedisKey.getCouponStockKey;
import static com.personblog.coupon.constant.RedisKey.getCouponUsersKey;

/**
 * 优惠券业务编排服务
 *
 * @author LSH
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponBizService {

    private final CouponTemplateService couponTemplateService;
    private final UserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;
    private final PointAPI pointAPI;
    private static final DefaultRedisScript<Long> COUPON_CLAIM_SCRIPT = new DefaultRedisScript<>();
    private final RabbitTemplate rabbitTemplate;

    static {
        COUPON_CLAIM_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/coupon_claim.lua")));
        COUPON_CLAIM_SCRIPT.setResultType(Long.class);
    }

    /**
     * 获取优惠券专区列表（分页）
     * 仅查询上架且在有效期内的优惠券模板
     * 已登录用户会填充 claimed 和 claimedCount
     */
    public Page<CouponZoneVO> getZoneList(CouponZoneQueryDTO queryDTO) {
        // 1. 构建分页参数
        Page<CouponTemplate> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 2. 构建查询条件：上架 + 抢购未结束 + 类型筛选
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<CouponTemplate>()
                // 仅查询上架状态
                .eq(CouponTemplate::getStatus, (short) 1)
                // 抢购未结束：claimEndTime 为 null 或 claimEndTime > 当前时间
                .and(w -> w.isNull(CouponTemplate::getClaimEndTime)
                        .or()
                        .gt(CouponTemplate::getClaimEndTime, now))
                // 按类型筛选（可选）
                .eq(queryDTO.getType() != null && queryDTO.getType() != 0, CouponTemplate::getClaimType, queryDTO.getType())
                // 按创建时间倒序
                .orderByDesc(CouponTemplate::getCreatedAt);

        // 3. 执行分页查询
        couponTemplateService.page(page, wrapper);

        // 4. 转换为 VO
        List<CouponZoneVO> voList = page.getRecords().stream()
                .map(t -> BeanUtil.copyProperties(t, CouponZoneVO.class))
                .collect(Collectors.toList());

        // 5. 已登录用户：填充领取状态（批量查询，避免循环N+1）
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            // 收集所有模板ID
            List<Long> templateIds = voList.stream()
                    .map(CouponZoneVO::getId)
                    .collect(Collectors.toList());

            // 批量查询：当前用户对这些模板的未使用优惠券
            List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                    .select(UserCoupon::getCouponTemplateId)
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getStatus, (short) 0)
                    .in(UserCoupon::getCouponTemplateId, templateIds)
                    .list();

            // 构建 Map<templateId, count>
            Map<Long, Integer> claimedCountMap = userCoupons.stream()
                    .collect(Collectors.groupingBy(UserCoupon::getCouponTemplateId, Collectors.summingInt(e -> 1)));

            // 填充到VO
            for (CouponZoneVO vo : voList) {
                vo.setClaimed(claimedCountMap.getOrDefault(vo.getId(), 0) > 0);
            }
        } else {
            // 未登录场景：默认未领取
            voList.forEach(vo -> vo.setClaimed(false));
        }

        // 6. 构建返回分页结果
        Page<CouponZoneVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 获取优惠券模板详情
     * 未登录用户 claimed 默认为 false
     */
    public CouponDetailVO getCouponDetail(Long id) {
        // 1. 查询优惠券模板
        CouponTemplate template = couponTemplateService.getById(id);
        if (template == null) {
            throw new BizException(BizCodeEnum.COUPON_NOT_EXIST);
        }

        // 2. 转换为 VO
        CouponDetailVO vo = BeanUtil.copyProperties(template, CouponDetailVO.class);

        // 3. 已登录用户：填充领取状态
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            long count = userCouponService.lambdaQuery()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponTemplateId, id)
                    .eq(UserCoupon::getStatus, (short) 0)
                    .count();
            vo.setClaimed(count > 0);
        } else {
            // 未登录：默认未领取
            vo.setClaimed(false);
        }

        return vo;
    }

    //抢券
    public void claimCoupon(CouponClaimDTO dto) {
        Long userId = UserContextHolder.getUserId();
        Long couponId = dto.getCouponTemplateId();
        String userCouponKey = getCouponUsersKey(couponId);

        // 前置校验：登录、券状态（不做重复领取检查，留给后续原子操作）
        CouponTemplate coupon = valid(userCouponKey, userId, couponId);

        boolean isUnlimited = coupon.getTotalCount() == null;

        // 【并发安全】先通过 Redis 原子操作完成去重+扣库存，再冻积分
        if (isUnlimited) {
            // 无限量券：SADD 原子去重（返回 1 表示新加入，0 表示已存在）
            Long added = redisTemplate.opsForSet().add(userCouponKey, userId.toString());
            if (added == null || added == 0) {
                throw new BizException(COUPON_ALREADY_CLAIMED);
            }
        } else {
            // 限量券：Lua 原子去重 + 扣库存（一次完成，不会并发超领）
            String stockKey = getCouponStockKey(couponId);
            Long res = redisTemplate.execute(
                    COUPON_CLAIM_SCRIPT,
                    List.of(stockKey, userCouponKey),
                    userId.toString(),
                    String.valueOf(3600));

            if (res == null) {
                throw new BizException(BizCodeEnum.OPERATION_ERROR);
            }
            if (res == 1) {
                throw new BizException(COUPON_ALREADY_CLAIMED);
            }
            if (res == 2) {
                throw new BizException(BizCodeEnum.COUPON_OUT_OF_STOCK);
            }
        }

        // 【并发安全】Redis 去重成功后，再冻结积分；失败则回滚 Redis 标记
        Integer pointsCost = coupon.getPointsCost();
        if (pointsCost != null && pointsCost > 0) {
            boolean frozenPoints = pointAPI.freezePoints(userId, pointsCost);
            if (!frozenPoints) {
                // 积分不足，回滚 Redis 去重标记，避免用户状态异常
                rollbackRedisClaim(userCouponKey, couponId, userId, isUnlimited);
                throw new BizException(COUPON_POINT_NOT_ENOUGH);
            }
        }

        // 计算用户优惠券过期时间：validDays 优先，否则用模板 endTime
        LocalDateTime userExpireTime;
        if (coupon.getValidDays() != null && coupon.getValidDays() > 0) {
            userExpireTime = LocalDateTime.now().plusDays(coupon.getValidDays());
        } else {
            userExpireTime = coupon.getEndTime();
        }

        // 发送 MQ 消息异步落库
        CouponClaimMessageDTO messageDTO = CouponClaimMessageDTO.builder()
                .couponType(coupon.getCouponType())
                .couponTemplateId(couponId)
                .couponName(coupon.getCouponName())
                .discountAmount(coupon.getDiscountAmount())
                .discountRate(coupon.getDiscountRate())
                .expireTime(userExpireTime)
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .pointsCost(coupon.getPointsCost())
                .sourceType(coupon.getClaimType())
                .userId(userId)
                .createTime(LocalDateTime.now())
                .build();
        rabbitTemplate.convertAndSend(COUPON_EXCHANGE, COUPON_CLAIM_KEY, messageDTO);
    }

    /**
     * 回滚 Redis 中的领取标记（积分冻结失败时调用）
     * 无限量券：SREM 移除用户标记
     * 限量券：INCR 恢复库存 + SREM 移除用户标记
     */
    private void rollbackRedisClaim(String userCouponKey, Long couponId, Long userId, boolean isUnlimited) {
        try {
            // 移除用户已领取标记
            redisTemplate.opsForSet().remove(userCouponKey, userId.toString());
            if (!isUnlimited) {
                // 限量券：恢复库存
                String stockKey = getCouponStockKey(couponId);
                redisTemplate.opsForValue().increment(stockKey);
            }
            log.warn("回滚 Redis 领取标记成功: couponId={}, userId={}", couponId, userId);
        } catch (Exception e) {
            // 回滚失败需要记录，后续可通过定时任务补偿
            log.error("回滚 Redis 领取标记失败: couponId={}, userId={}", couponId, userId, e);
        }
    }

    //前置校验
    private CouponTemplate valid(String userCouponKey, Long userId, Long couponId) {
        // 未登录
        if (userId == null) {
            throw new BizException(BizCodeEnum.NOT_LOGIN);
        }

        // 已领取过（Redis 快速拦截）
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(userCouponKey, userId.toString()))) {
            throw new BizException(COUPON_ALREADY_CLAIMED);
        }

        // 优惠券是否存在
        CouponTemplate coupon = couponTemplateService.getById(couponId);
        if (coupon == null) {
            throw new BizException(BizCodeEnum.COUPON_NOT_EXIST);
        }

        // 是否上架
        if (coupon.getStatus() != null && coupon.getStatus() != 1) {
            throw new BizException(COUPON_NOT_ON_SHELF);
        }

        LocalDateTime now = LocalDateTime.now();

        // 抢购是否未开始
        if (coupon.getClaimStartTime() != null && coupon.getClaimStartTime().isAfter(now)) {
            throw new BizException(COUPON_NOT_STARTED);
        }

        // 抢购是否已结束
        if (coupon.getClaimEndTime() != null && coupon.getClaimEndTime().isBefore(now)) {
            throw new BizException(BizCodeEnum.COUPON_EXPIRED);
        }

        // 积分是否充足
        Integer pointsCost = coupon.getPointsCost();
        if (pointsCost != null && pointsCost > 0) {
            Long availablePoints = pointAPI.getPointInfo(userId).getAvailablePoints();
            if (availablePoints < pointsCost) {
                throw new BizException(BizCodeEnum.COUPON_POINT_NOT_ENOUGH);
            }
        }

        return coupon;
    }
}
