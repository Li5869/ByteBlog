package com.personblog.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.admin.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签表 Mapper 接口
 *
 * @author LSH
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

}
