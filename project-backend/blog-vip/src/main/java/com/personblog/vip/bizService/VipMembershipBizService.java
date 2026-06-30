package com.personblog.vip.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.vip.entity.VipMembership;
import com.personblog.vip.service.IVipMembershipService;
import com.personblog.vip.vo.VipInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 会员状态业务编排层
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipMembershipBizService {

    private final IVipMembershipService vipMembershipService;

    /**
     * 获取当前用户会员信息
     */
    public VipInfoVO getMembership() {
        Long userId = UserContextHolder.getUserId();
        // 查询当前用户的VIP会员记录
        VipMembership membership = vipMembershipService.getOne(
                new LambdaQueryWrapper<VipMembership>()
                        .eq(VipMembership::getUserId, userId)
        );

        // 从未开通过VIP
        if (membership == null) {
            return VipInfoVO.builder()
                    .isVip(false)
                    .vipLevel((short) 0)
                    .build();
        }

        // 先拷贝同名字段，再手动设置计算字段
        VipInfoVO vo = BeanUtil.copyProperties(membership, VipInfoVO.class);

        // 判断是否过期：VIP等级为1且到期时间在当前时间之后
        LocalDateTime now = LocalDateTime.now();
        boolean isVip = membership.getVipLevel() == 1
                && membership.getEndTime() != null
                && membership.getEndTime().isAfter(now);
        vo.setIsVip(isVip);

        // 计算剩余天数
        if (isVip) {
            vo.setRemainDays((int) ChronoUnit.DAYS.between(now, membership.getEndTime()));
        }

        return vo;
    }

    /**
     * 激活VIP会员（新开通或续费）
     *
     * @param userId 用户ID
     * @param durationMonths 开通月数
     * @param orderId 订单ID（用于记录最近开通订单）
     */
    public void activateVip(Long userId, Integer durationMonths, Long orderId) {
        LocalDateTime now = LocalDateTime.now();

        // 查询用户当前会员记录
        VipMembership membership = vipMembershipService.getOne(
                new LambdaQueryWrapper<VipMembership>()
                        .eq(VipMembership::getUserId, userId)
        );

        if (membership == null) {
            // 新开通会员
            membership = new VipMembership();
            membership.setUserId(userId);
            membership.setVipLevel((short) 1);
            membership.setStartTime(now);
            membership.setEndTime(now.plusMonths(durationMonths));
            membership.setTotalMonths(durationMonths);
            membership.setLastOrderId(orderId);
            vipMembershipService.save(membership);
            log.info("新开通VIP会员: userId={}, orderId={}, durationMonths={}, endTime={}",
                    userId, orderId, durationMonths, membership.getEndTime());
        } else {
            // 已有会员记录，判断是否过期
            boolean isExpired = membership.getEndTime() == null || membership.getEndTime().isBefore(now);

            if (isExpired) {
                // 会员已过期，重新开通
                membership.setStartTime(now);
                membership.setEndTime(now.plusMonths(durationMonths));
            } else {
                // 会员未过期，续费延长
                membership.setEndTime(membership.getEndTime().plusMonths(durationMonths));
            }

            // 更新会员等级、累计月数和最近订单ID
            membership.setVipLevel((short) 1);
            membership.setTotalMonths(membership.getTotalMonths() + durationMonths);
            membership.setLastOrderId(orderId);
            vipMembershipService.updateById(membership);
            log.info("续费VIP会员: userId={}, orderId={}, durationMonths={}, newEndTime={}, isExpired={}",
                    userId, orderId, durationMonths, membership.getEndTime(), isExpired);
        }
    }
    //confirm失败，退回会员
    public void deactivateVip(Long userId) {
        vipMembershipService.remove(new LambdaQueryWrapper<VipMembership>()
                .eq(VipMembership::getUserId,userId));
    }
}
