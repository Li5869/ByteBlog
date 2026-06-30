package com.personblog.point.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.point.entity.UserPoint;
import com.personblog.point.mapper.UserPointMapper;
import com.personblog.point.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户积分表 服务实现类
 *
 * @author LSH
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPointServiceImpl extends ServiceImpl<UserPointMapper, UserPoint> implements UserPointService {
}
