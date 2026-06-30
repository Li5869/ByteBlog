package com.personblog.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.interaction.entity.ArticleLike;
import com.personblog.interaction.vo.MyLikeVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章点赞 Mapper
 *
 * @author LSH
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    Page<MyLikeVO> selectMyLikes(Page<MyLikeVO> page, Long userId);
}
