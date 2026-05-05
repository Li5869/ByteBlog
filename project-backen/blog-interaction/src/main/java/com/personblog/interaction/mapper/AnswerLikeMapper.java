package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.AnswerLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 回答点赞 Mapper
 *
 * @author LSH
 */
@Mapper
public interface AnswerLikeMapper extends BaseMapper<AnswerLike> {

}
