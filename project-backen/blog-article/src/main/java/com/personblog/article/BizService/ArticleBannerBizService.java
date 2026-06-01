package com.personblog.article.BizService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.article.entity.Article;
import com.personblog.article.service.IArticleService;
import com.personblog.article.vo.Article.BannerVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.personblog.article.config.cachConfig.ArticleCacheConfig.bannerCache;
import static com.personblog.article.constant.RedisKeys.ARTICLE_BANNERS;
import static com.personblog.common.constant.PageConstant.DEFAULT_BANNER_SIZE;
import static com.personblog.common.constant.StatusConstant.APPROVED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleBannerBizService {
    private final IArticleService articleService;

    @PostConstruct
    public void initCaches() {

    }

    /**
     * 获取Banner轮播图
     * 缓存策略：Caffeine 本地缓存，1小时
     * 前端固定展示3条轮播图，使用固定缓存key
     */
    public List<BannerVO> getBanners() {
        String cacheKey = ARTICLE_BANNERS;

        List<BannerVO> cached = bannerCache.getIfPresent(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            log.debug("Banner缓存命中");
            return cached;
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
                .eq(Article::getIsDeleted, false)
                .eq(Article::getReview, APPROVED)
                .eq(Article::getIsHot, true)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getViews);

        List<Article> articles = articleService.page(new Page<>(1, DEFAULT_BANNER_SIZE), wrapper).getRecords();
        List<BannerVO> result = articles.stream().map(article -> {
            BannerVO vo = new BannerVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            vo.setCover(article.getCover());
            vo.setArticleId(article.getId());
            return vo;
        }).collect(Collectors.toList());

        bannerCache.put(cacheKey, result);
        log.debug("Banner数据已缓存: {}", result.size());
        return result;
    }
}
