package com.personblog.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 评论表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}
