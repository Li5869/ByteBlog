package com.personblog.point.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.point.entity.PointRankLog;
import com.personblog.point.mapper.PointRankLogMapper;
import com.personblog.point.service.PointRankLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 积分排行榜月度记录表 服务实现类
 *
 * @author LSH
 * @since 2026-06-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PointRankLogServiceImpl extends ServiceImpl<PointRankLogMapper, PointRankLog> implements PointRankLogService {
}
