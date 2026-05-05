package com.personblog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.article.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文章标签关联表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

}
