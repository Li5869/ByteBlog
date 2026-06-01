package com.personblog.point.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.point.entity.PointLog;
import com.personblog.point.mapper.PointLogMapper;
import com.personblog.point.service.PointLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 积分流水表 服务实现类
 *
 * @author LSH
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointLogServiceImpl extends ServiceImpl<PointLogMapper, PointLog> implements PointLogService {
}
