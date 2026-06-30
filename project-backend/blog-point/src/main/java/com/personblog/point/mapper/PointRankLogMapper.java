package com.personblog.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.point.entity.PointRankLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分排行榜月度记录表 Mapper 接口
 *
 * @author LSH
 * @since 2026-06-01
 */
@Mapper
public interface PointRankLogMapper extends BaseMapper<PointRankLog> {
}
