package com.personblog.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personblog.article.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 随机获取指定数量的已发布且审核通过的文章
     * 置顶文章优先显示，其他文章随机排序
     * @param limit 返回数量
     * @return 文章列表（置顶在前，其他随机）
     */
    @Select("SELECT * FROM tb_article WHERE status = 1 AND is_deleted = false AND review = 'approved' ORDER BY is_top DESC, RANDOM() LIMIT #{limit}")
    List<Article> selectRandomArticles(@Param("limit") int limit);
}
