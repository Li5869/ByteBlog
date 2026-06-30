package com.personblog.vip.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.vip.entity.VipPlan;
import com.personblog.vip.mapper.VipPlanMapper;
import com.personblog.vip.service.IVipPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * VipPlan 服务实现类
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VipPlanServiceImpl extends ServiceImpl<VipPlanMapper, VipPlan> implements IVipPlanService {
}
