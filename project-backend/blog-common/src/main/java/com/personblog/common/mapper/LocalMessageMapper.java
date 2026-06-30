package com.personblog.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.common.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper
 *
 * @author LSH
 * @since 2026-06-05
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {
}
