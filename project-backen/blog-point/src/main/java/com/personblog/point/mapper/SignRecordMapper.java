package com.personblog.point.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.point.entity.SignRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录表 Mapper 接口
 *
 * @author LSH
 * @since 2026-06-01
 */
@Mapper
public interface SignRecordMapper extends BaseMapper<SignRecord> {
}
