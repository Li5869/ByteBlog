package com.personblog.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.point.entity.UserPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分表 Mapper 接口
 *
 * @author LSH
 * @since 2026-06-01
 */
@Mapper
public interface UserPointMapper extends BaseMapper<UserPoint> {
}
