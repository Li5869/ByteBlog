package com.personblog.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.ai.entity.WritingPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 写作计划 Mapper
 *
 * @author LSH
 */
@Mapper
public interface WritingPlanMapper extends BaseMapper<WritingPlan> {
}
