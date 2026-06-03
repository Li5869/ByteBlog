package com.personblog.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.point.entity.PointLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水表 Mapper 接口
 *
 * @author LSH
 * @since 2026-06-01
 */
@Mapper
public interface PointLogMapper extends BaseMapper<PointLog> {
}
