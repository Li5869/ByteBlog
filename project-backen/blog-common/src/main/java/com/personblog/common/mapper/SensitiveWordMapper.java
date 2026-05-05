package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 敏感词表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

}
