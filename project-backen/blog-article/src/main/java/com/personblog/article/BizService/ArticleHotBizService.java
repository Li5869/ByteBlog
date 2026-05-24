package com.personblog.article.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.api.articleAPI.HotArticleAPI;
import com.personblog.article.entity.Article;
import com.personblog.article.mapper.ArticleMapper;
import com.personblog.article.service.IArticleService;
import com.personblog.article.vo.Article.HotArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.hotArticleCache;
import static com.personblog.common.constant.PageConstant.DEFAULT_HOT_SIZE;
import static com.personblog.common.constant.PageConstant.MAX_HOT_SIZE;
import static com.personblog.common.constant.RedisKeys.ARTICLE_HOT;
import static com.personblog.common.constant.RedisKeys.BROWSE_COUNT_KEY;
import static com.personblog.common.constant.StatusConstant.APPROVED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleHotBizService implements HotArticleAPI {
    private final IArticleService articleService;
    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;
    /**
     * 获取热门文章
     * 查询由定时任务 refreshHotArticles 预计算的 is_hot 标记
     * 缓存策略：Caffeine 本地缓存，5分钟
     */
    public List<HotArticleVO> getHotArticles(Integer size) {
        int limit = (size == null || size <= 0) ? DEFAULT_HOT_SIZE : Math.min(size, MAX_HOT_SIZE);
        String cacheKey = ARTICLE_HOT + limit;

        // 先查本地缓存
        List<HotArticleVO> cached = hotArticleCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("热门文章缓存命中");
            return cached;
        }

        // 查询 is_hot=true 的文章，按综合热度分降序
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getIsHot, true)
                .eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED)
                .orderByDesc(Article::getViews, Article::getLikes, Article::getComments, Article::getCollections);
        List<Article> articles = articleService.page(new Page<>(1, limit), wrapper).getRecords();

        List<HotArticleVO> result = articles.stream().map(article -> {
            HotArticleVO vo = new HotArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            // 实时浏览量 = DB基础值 + Redis未刷新增量
            long view = article.getViews() != null ? article.getViews() : 0L;
            Object browseCount = redisTemplate.opsForHash().get(BROWSE_COUNT_KEY, article.getId().toString());
            if (browseCount != null) {
                view += Long.parseLong(browseCount.toString());
            }
            vo.setViews(view);
            return vo;
        }).collect(Collectors.toList());

        // 写入本地缓存
        hotArticleCache.put(cacheKey, result);
        log.debug("热门文章已缓存: {}", result.size());

        return result;
    }

    /**
     * 刷新热门文章标记
     * 1. 清除所有 is_hot 标记
     * 2. 根据综合热度分重新标记 Top N
     * 3. 清除本地缓存
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshHotArticles() {
        articleMapper.clearAllHotFlags();
        articleMapper.refreshHotFlags(MAX_HOT_SIZE);
        hotArticleCache.invalidateAll();
        log.info("热门文章标记已刷新, Top {}", MAX_HOT_SIZE);
    }
}
