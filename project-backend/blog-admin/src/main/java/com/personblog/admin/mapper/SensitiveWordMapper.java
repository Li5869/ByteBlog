package com.personblog.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.admin.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词表 Mapper 接口
 *
 * @author LSH
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

}
