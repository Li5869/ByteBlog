package com.personblog.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.question.entity.QuestionTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问题-标签关联表 Mapper 接口
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface QuestionTagMapper extends BaseMapper<QuestionTag> {

}
