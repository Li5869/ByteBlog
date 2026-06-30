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
    @Select("SELECT * FROM tb_article WHERE status = 1 AND is_deleted = false AND review = 'approved' and is_hot != true ORDER BY is_top DESC, RANDOM() LIMIT #{limit}")
    List<Article> selectRandomArticles(@Param("limit") int limit);

    /**
     * 批量清除热门标记
     */
    void clearAllHotFlags();

    /**
     * 根据综合热度分刷新 Top N 的热门标记
     * 热度分 = views + likes*10 + comments*5 + collections*8
     * @param limit 刷新数量
     */
    void refreshHotFlags(@Param("limit") int limit);
}
