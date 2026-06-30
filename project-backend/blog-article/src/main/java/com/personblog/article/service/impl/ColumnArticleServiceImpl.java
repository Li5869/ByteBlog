package com.personblog.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.article.entity.ColumnArticle;
import com.personblog.article.mapper.ColumnArticleMapper;
import com.personblog.article.service.IColumnArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 专栏文章关联服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ColumnArticleServiceImpl extends ServiceImpl<ColumnArticleMapper, ColumnArticle> implements IColumnArticleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAddArticles(Long columnId, List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return 0;
        }

        // 查询已存在的文章ID，避免重复添加
        List<Long> existingArticleIds = lambdaQuery()
                .eq(ColumnArticle::getColumnId, columnId)
                .in(ColumnArticle::getArticleId, articleIds)
                .list()
                .stream()
                .map(ColumnArticle::getArticleId)
                .toList();

        Set<Long> existingSet = new HashSet<>(existingArticleIds);

        // 过滤出不存在的文章ID
        List<Long> newArticleIds = articleIds.stream()
                .filter(id -> !existingSet.contains(id))
                .toList();

        if (newArticleIds.isEmpty()) {
            return 0;
        }

        // 批量创建关联记录
        LocalDateTime now = LocalDateTime.now();
        List<ColumnArticle> columnArticles = new ArrayList<>();
        for (Long articleId : newArticleIds) {
            ColumnArticle ca = new ColumnArticle();
            ca.setColumnId(columnId);
            ca.setArticleId(articleId);
            ca.setCreatedAt(now);
            columnArticles.add(ca);
        }

        saveBatch(columnArticles);
        return columnArticles.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemoveArticles(Long columnId, List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<ColumnArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnArticle::getColumnId, columnId)
                .in(ColumnArticle::getArticleId, articleIds);

        return baseMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int removeArticleFromAllColumns(Long articleId) {
        LambdaQueryWrapper<ColumnArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ColumnArticle::getArticleId, articleId);

        return baseMapper.delete(wrapper);
    }

    @Override
    public List<Long> getArticleIdsByColumnId(Long columnId) {
        return lambdaQuery()
                .eq(ColumnArticle::getColumnId, columnId)
                .select(ColumnArticle::getArticleId)
                .list()
                .stream()
                .map(ColumnArticle::getArticleId)
                .toList();
    }

    @Override
    public boolean existsByColumnIdAndArticleId(Long columnId, Long articleId) {
        return lambdaQuery()
                .eq(ColumnArticle::getColumnId, columnId)
                .eq(ColumnArticle::getArticleId, articleId)
                .exists();
    }

    @Override
    public int countByColumnId(Long columnId) {
        return lambdaQuery()
                .eq(ColumnArticle::getColumnId, columnId)
                .count()
                .intValue();
    }
}
