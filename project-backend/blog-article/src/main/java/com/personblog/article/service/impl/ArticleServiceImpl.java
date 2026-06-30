package com.personblog.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.articleAPI.ArticleAPI;
import com.personblog.article.entity.Article;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.service.IArticleService;
import com.personblog.common.utils.MultiLevelCacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.personblog.article.constant.RedisKeys.ARTICLE_DETAIL;
import static com.personblog.common.constant.StatusConstant.PENDING;


/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author LSH
 * @since 2026-03-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService, ArticleAPI {
    private final MultiLevelCacheUtil cacheUtil;
    @Override
    public Long getArticleAuthorId(Long articleId) {
        return getById(articleId).getAuthorId();
    }

    @Override
    public void updateArticleReviewStatus(Long articleId, String status) {
        boolean update = lambdaUpdate()
                .eq(Article::getId, articleId)
                .eq(Article::getReview, PENDING)
                .set(Article::getReview, status)
                .update();
        if (update) {
            log.info("文章审核状态更新成功: articleId={}, status={}", articleId, status);
            // 清除文章详情缓存
            cacheUtil.evict(ARTICLE_DETAIL + articleId);
        } else {
            log.warn("文章审核状态更新失败，文章不存在或已审核: articleId={}", articleId);
        }
    }
    @Override
    public String getArticleTitle(Long articleId) {
        Article article = getById(articleId);
        return article != null ? article.getTitle() : null;
    }
}
