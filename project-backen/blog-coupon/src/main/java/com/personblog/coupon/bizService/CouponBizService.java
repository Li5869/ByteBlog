package com.personblog.coupon.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import com.personblog.coupon.dto.CouponZoneQueryDTO;
import com.personblog.coupon.dto.MyCouponQueryDTO;
import com.personblog.coupon.entity.CouponTemplate;
import com.personblog.coupon.entity.UserCoupon;
import com.personblog.coupon.service.CouponTemplateService;
import com.personblog.coupon.service.UserCouponService;
import com.personblog.coupon.vo.CouponDetailVO;
import com.personblog.coupon.vo.CouponZoneVO;
import com.personblog.coupon.vo.MyCouponStatsVO;
import com.personblog.coupon.vo.MyCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 获取优惠券专区列表（分页）
     * 仅查询上架且在有效期内的优惠券模板
     * 已登录用户会填充 claimed 和 claimedCount
     */
    public Page<CouponZoneVO> getZoneList(CouponZoneQueryDTO queryDTO) {
        // 1. 构建分页参数
        Page<CouponTemplate> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 2. 构建查询条件：上架 + 有效期内 + 类型筛选
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<CouponTemplate>()
                // 仅查询上架状态
                .eq(CouponTemplate::getStatus, (short) 1)
                // 未设置结束时间或结束时间大于当前时间
                .and(w -> w.isNull(CouponTemplate::getEndTime)
                        .or()
                        .gt(CouponTemplate::getEndTime, LocalDateTime.now()))
                // 按类型筛选（可选）
                .eq(queryDTO.getType() != null && queryDTO.getType() != 0, CouponTemplate::getClaimType, queryDTO.getType().shortValue())
                // 按创建时间倒序
                .orderByDesc(CouponTemplate::getCreatedAt);

        // 3. 执行分页查询
        couponTemplateService.page(page, wrapper);

        // 4. 转换为 VO
        List<CouponZoneVO> voList = page.getRecords().stream()
                .map(t -> BeanUtil.copyProperties(t, CouponZoneVO.class))
                .collect(Collectors.toList());

        // 5. 已登录用户：填充领取状态
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            for (CouponZoneVO vo : voList) {
                // VO 的 id 已对齐为 Long，无需再做字符串转换
                Long templateId = vo.getId();
                // 查询当前用户对该模板的未使用优惠券数量
                long count = userCouponService.lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponTemplateId, templateId)
                        .eq(UserCoupon::getStatus, (short) 0)
                        .count();
                vo.setClaimed(count > 0);
                vo.setClaimedCount((int) count);
            }
        } else {
            // 未登录场景：默认未领取
            voList.forEach(vo -> {
                vo.setClaimed(false);
                vo.setClaimedCount(0);
            });
        }

        // 6. 构建返回分页结果
        Page<CouponZoneVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 获取当前用户的优惠券列表（分页）
     * 需要登录
     */
    public Page<MyCouponVO> getMyCoupons(MyCouponQueryDTO queryDTO) {
        // 获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();

        // 1. 构建分页参数
        Page<UserCoupon> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<UserCoupon> wrapper = null;
        if (queryDTO.getStatus() != null) {
            wrapper = new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    // 状态筛选（可选）
                    .eq(queryDTO.getStatus() != null, UserCoupon::getStatus, queryDTO.getStatus().shortValue())
                    // 按领取时间倒序
                    .orderByDesc(UserCoupon::getObtainTime);
        }

        // 3. 执行分页查询
        userCouponService.page(page, wrapper);

        // 4. 转换为 VO
        Page<MyCouponVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<MyCouponVO> voList = page.getRecords().stream()
                .map(u -> BeanUtil.copyProperties(u, MyCouponVO.class))
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 获取当前用户优惠券统计
     * 包含总数、未使用、已使用、已过期数量
     */
    public MyCouponStatsVO getMyCouponStats() {
        // 获取当前登录用户ID
        Long userId = UserContextHolder.getUserId();

        // 分别统计各状态数量
        long total = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId).count();
        long unused = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 0).count();
        long used = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 1).count();
        long expired = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, (short) 2).count();

        return MyCouponStatsVO.builder()
                .total((int) total)
                .unused((int) unused)
                .used((int) used)
                .expired((int) expired)
                .build();
    }

    /**
     * 获取优惠券模板详情
     * 未登录用户 claimed/claimedCount 默认为 false/0
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
            vo.setClaimedCount((int) count);
        } else {
            // 未登录：默认未领取
            vo.setClaimed(false);
            vo.setClaimedCount(0);
        }

        return vo;
    }
}
