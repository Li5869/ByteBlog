package com.personblog.coupon.config;

import com.personblog.coupon.constant.RedisKey;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.CouponTemplateService;
import com.personblog.coupon.service.UserCouponService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 优惠券缓存预热
 * 服务启动时将所有上架券的库存和已领取用户集合加载到 Redis
 *
 * @author LSH
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponCacheWarmer {

    private final CouponTemplateService couponTemplateService;
    private final UserCouponService userCouponService;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void warmUp() {
        try {
            log.info("开始预热优惠券缓存...");

            // 1. 查询所有上架且未过期的优惠券模板
            List<CouponTemplate> templates = couponTemplateService.lambdaQuery()
                    .eq(CouponTemplate::getStatus, (short) 1)
                    // 抢购未结束：claimEndTime 为 null 或 claimEndTime > 当前时间
                    .and(w -> w.isNull(CouponTemplate::getClaimEndTime)
                            .or()
                            .gt(CouponTemplate::getClaimEndTime, java.time.LocalDateTime.now()))
                    .list();

            if (templates.isEmpty()) {
                log.info("无需预热的优惠券");
                return;
            }

            int stockCount = 0;
            int userSetCount = 0;

            for (CouponTemplate template : templates) {
                Long templateId = template.getId();

                // 2. 预热库存（仅限量的券才写入 Redis）
                if (template.getStock() != null && template.getTotalCount() != null) {
                    String stockKey = RedisKey.getCouponStockKey(templateId);
                    redisTemplate.opsForValue().set(stockKey, String.valueOf(template.getStock()), 1, TimeUnit.HOURS);
                    stockCount++;
                }

                // 3. 预热已领取用户集合
                List<Long> claimedUserIds = userCouponService.lambdaQuery()
                        .select(UserCoupon::getUserId)
                        .eq(UserCoupon::getCouponTemplateId, templateId)
                        .eq(UserCoupon::getStatus, (short) 0)
                        .list()
                        .stream()
                        .map(UserCoupon::getUserId)
                        .toList();

                if (!claimedUserIds.isEmpty()) {
                    String usersKey = RedisKey.getCouponUsersKey(templateId);
                    redisTemplate.opsForSet().add(usersKey,
                            claimedUserIds.stream().map(String::valueOf).toArray(String[]::new));
                    redisTemplate.expire(usersKey, 1, TimeUnit.HOURS);
                    userSetCount++;
                }
            }

            log.info("优惠券缓存预热完成，共处理 {} 张券（库存缓存 {} 个，用户集合 {} 个）",
                    templates.size(), stockCount, userSetCount);

        } catch (Exception e) {
            log.error("优惠券缓存预热失败，不影响服务启动", e);
        }
    }
}
