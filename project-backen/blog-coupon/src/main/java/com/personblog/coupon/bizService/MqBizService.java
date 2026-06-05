package com.personblog.coupon.bizService;

import com.personblog.api.pointAPI.PointAPI;
import com.personblog.common.dto.MqMessage.Coupon.CouponClaimMessageDTO;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.CouponTemplateService;
import com.personblog.coupon.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.personblog.common.constant.PointTypeConstants.EXCHANGE;

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

    /**
     * 处理优惠券领取消息
     * 1. 写入用户优惠券记录
     * 2. 扣减模板库存（限量券）
     * 3. 积分兑换券扣减用户积分
     *
     * @param message MQ 消息体
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleCouponClaim(CouponClaimMessageDTO message) {
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

        userCouponService.save(userCoupon);
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
                log.warn("库存扣减失败（库存已为0）: couponTemplateId={}", message.getCouponTemplateId());
            }
        }

        // 3. 积分兑换券：确认扣减积分
        if (message.getSourceType() != null && message.getSourceType() == 2
                && message.getPointsCost() != null && message.getPointsCost() > 0) {
            pointAPI.confirmDeductPoints(
                    message.getUserId(),
                    message.getPointsCost(),
                    EXCHANGE,
                    userCoupon.getId(),
                    "积分兑换优惠券: " + message.getCouponName()
            );
            log.info("积分确认扣减成功: userId={}, points={}", message.getUserId(), message.getPointsCost());
        }
    }
    public void cancelPoint(Long userId, Integer pointsCost) {
        pointAPI.cancelDeductPoints(userId,pointsCost);
    }
}
