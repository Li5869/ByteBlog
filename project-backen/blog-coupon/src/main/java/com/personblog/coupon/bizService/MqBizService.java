package com.personblog.coupon.bizService;

import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.dto.MqMessage.Coupon.CouponClaimMessageDTO;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.CouponTemplateService;
import com.personblog.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.personblog.common.constant.PointTypeConstants.EXCHANGE;
import static com.personblog.coupon.constant.RedisKey.getCouponStockKey;
import static com.personblog.coupon.constant.RedisKey.getCouponUsersKey;

/**
 * 优惠券 MQ 消息业务处理服务
 * 负责优惠券领取消息的异步落库（写用户优惠券记录 + 扣库存 + 扣积分）
 *
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqBizService {

    private final UserCouponService userCouponService;
    private final CouponTemplateService couponTemplateService;
    private final PointAPI pointAPI;
    private final StringRedisTemplate redisTemplate;

    /**
     * 处理优惠券领取消息（幂等）
     * 1. 幂等校验（代码层 + 唯一索引兜底）
     * 2. 写入用户优惠券记录
     * 3. 扣减模板库存（限量券），DB 库存不足时回补 Redis
     * 4. 积分兑换券扣减用户积分
     *
     * @param message MQ 消息体
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCouponClaim(CouponClaimMessageDTO message) {
        // 0. 幂等校验：已存在则直接返回（代码层快速判断）
        boolean exists = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, message.getUserId())
                .eq(UserCoupon::getCouponTemplateId, message.getCouponTemplateId())
                .exists();
        if (exists) {
            log.info("重复消费，跳过: userId={}, couponTemplateId={}", message.getUserId(), message.getCouponTemplateId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 构建并写入用户优惠券记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(message.getUserId());
        userCoupon.setCouponTemplateId(message.getCouponTemplateId());
        userCoupon.setCouponName(message.getCouponName());
        userCoupon.setCouponType(message.getCouponType());
        userCoupon.setDiscountAmount(message.getDiscountAmount());
        userCoupon.setDiscountRate(message.getDiscountRate());
        userCoupon.setMinOrderAmount(message.getMinOrderAmount());
        userCoupon.setStatus((short) 0); // 未使用
        userCoupon.setSourceType(message.getSourceType());
        userCoupon.setObtainTime(now);
        userCoupon.setExpireTime(message.getExpireTime());
        userCoupon.setCreatedAt(now);
        userCoupon.setUpdatedAt(now);

        try {
            userCouponService.save(userCoupon);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发场景下代码层幂等校验通过但 INSERT 冲突，直接跳过
            log.info("唯一索引命中，重复消费跳过: userId={}, couponTemplateId={}", message.getUserId(), message.getCouponTemplateId());
            return;
        }
        log.info("优惠券落库成功: userId={}, couponTemplateId={}, userCouponId={}",
                message.getUserId(), message.getCouponTemplateId(), userCoupon.getId());

        // 2. 扣减模板库存（仅限量券，stock != null）
        CouponTemplate template = couponTemplateService.getById(message.getCouponTemplateId());
        if (template != null && template.getStock() != null) {
            boolean update = couponTemplateService.lambdaUpdate()
                    .eq(CouponTemplate::getId, message.getCouponTemplateId())
                    .gt(CouponTemplate::getStock, 0)  // 防御性：stock > 0 才扣
                    .setSql("stock = stock - 1")
                    .update();
            if (update) {
                log.info("库存扣减成功: couponTemplateId={}", message.getCouponTemplateId());
            } else {
                // DB 库存已为 0，回补 Redis 库存（防止 Redis 和 DB 不一致）
                redisTemplate.opsForValue().increment(getCouponStockKey(message.getCouponTemplateId()));
                // 从已领取集合中移除该用户（回滚 Redis 去重标记）
                redisTemplate.opsForSet().remove(getCouponUsersKey(message.getCouponTemplateId()), message.getUserId().toString());
                log.warn("DB 库存不足，已回补 Redis: couponTemplateId={}, userId={}", message.getCouponTemplateId(), message.getUserId());
                // 抛异常触发事务回滚，删除刚插入的 user_coupon 记录
                throw new RuntimeException("DB 库存不足，回滚本次领取");
            }
        }

        // 3. 积分兑换券：扣减积分（负数表示减少）
        if (message.getSourceType() != null && message.getSourceType() == 2
                && message.getPointsCost() != null && message.getPointsCost() > 0) {
            // 使用 changePoint 方法，传入负数扣减积分
            pointAPI.changePoint(
                    message.getUserId(),
                    -message.getPointsCost(),  // 负数表示扣减
                    EXCHANGE,
                    userCoupon.getId(),
                    "积分兑换优惠券: " + message.getCouponName()
            );
            log.info("积分扣减成功: userId={}, points={}", message.getUserId(), message.getPointsCost());
        }
    }
}
