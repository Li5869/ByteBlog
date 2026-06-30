package com.personblog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.article.entity.ColumnArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 专栏文章关联Mapper接口
 *
 * @author LSH
 */
@Mapper
public interface ColumnArticleMapper extends BaseMapper<ColumnArticle> {

}
