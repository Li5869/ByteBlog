package com.personblog.vip.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.vip.entity.VipMembership;
import com.personblog.vip.mapper.VipMembershipMapper;
import com.personblog.vip.service.IVipMembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * VipMembership 服务实现类
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VipMembershipServiceImpl extends ServiceImpl<VipMembershipMapper, VipMembership> implements IVipMembershipService {
}
