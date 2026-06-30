package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.interaction.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞 Mapper
 *
 * @author LSH
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

}
