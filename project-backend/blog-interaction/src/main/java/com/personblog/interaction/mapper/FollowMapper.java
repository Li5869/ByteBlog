package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 关注表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

}
