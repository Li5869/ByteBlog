package com.personblog.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.article.entity.ColumnArticle;

import java.util.List;

/**
 * 专栏文章关联服务接口
 *
 * @author LSH
 */
public interface IColumnArticleService extends IService<ColumnArticle> {

    /**
     * 批量添加文章到专栏
     * @param columnId 专栏ID
     * @param articleIds 文章ID列表
     * @return 添加成功的数量
     */
    int batchAddArticles(Long columnId, List<Long> articleIds);

    /**
     * 批量从专栏移除文章
     * @param columnId 专栏ID
     * @param articleIds 文章ID列表
     * @return 移除成功的数量
     */
    int batchRemoveArticles(Long columnId, List<Long> articleIds);

    /**
     * 删除文章时，从所有专栏中移除该文章
     * @param articleId 文章ID
     * @return 移除的专栏数量
     */
    int removeArticleFromAllColumns(Long articleId);

    /**
     * 获取专栏中的文章ID列表
     * @param columnId 专栏ID
     * @return 文章ID列表
     */
    List<Long> getArticleIdsByColumnId(Long columnId);

    /**
     * 检查文章是否在专栏中
     * @param columnId 专栏ID
     * @param articleId 文章ID
     * @return 是否存在
     */
    boolean existsByColumnIdAndArticleId(Long columnId, Long articleId);

    /**
     * 统计专栏中的文章数量
     * @param columnId 专栏ID
     * @return 文章数量
     */
    int countByColumnId(Long columnId);
}
