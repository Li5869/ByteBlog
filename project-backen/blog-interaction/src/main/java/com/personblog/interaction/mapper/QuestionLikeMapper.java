package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.QuestionLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问题点赞 Mapper
 *
 * @author LSH
 */
@Mapper
public interface QuestionLikeMapper extends BaseMapper<QuestionLike> {

}
