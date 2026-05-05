package com.personblog.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.question.entity.Answer;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 回答表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface AnswerMapper extends BaseMapper<Answer> {

}
