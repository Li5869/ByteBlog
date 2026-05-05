package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 标签表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

}
