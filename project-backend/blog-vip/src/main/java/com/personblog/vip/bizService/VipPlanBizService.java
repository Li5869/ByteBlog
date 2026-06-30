package com.personblog.vip.bizService;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.service.IVipPlanService;
import com.personblog.vip.vo.VipPlanVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * VIP 套餐业务编排层
 * @author LSH
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipPlanBizService {

    private final IVipPlanService vipPlanService;

    /**
     * 查询所有上架套餐（按排序权重升序）
     */
    public List<VipPlanVO> listPlans() {
        List<VipPlan> plans = vipPlanService.list(
                new LambdaQueryWrapper<VipPlan>()
                        .eq(VipPlan::getStatus, (short) 1)
                        .orderByAsc(VipPlan::getSortOrder)
        );
        return plans.stream()
                .map(p -> BeanUtil.copyProperties(p, VipPlanVO.class))
                .toList();
    }
}
