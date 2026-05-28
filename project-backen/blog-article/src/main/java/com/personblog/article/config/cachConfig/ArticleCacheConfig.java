package com.personblog.article.config.cachConfig;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.article.vo.Article.ArticleListVO;
import com.personblog.article.vo.Article.BannerVO;
import com.personblog.article.vo.Article.HotArticleVO;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class ArticleCacheConfig {
    // 本地缓存 - 文章分页列表
    public static Cache<String, Page<ArticleListVO>> articlePageCache;
    // 本地缓存 - Banner
    public static Cache<String, List<BannerVO>> bannerCache;
    // 本地缓存 - 热门文章
    public static Cache<String, List<HotArticleVO>> hotArticleCache;
    @PostConstruct
    public void initCaches() {
        // Banner 本地缓存
        bannerCache = Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();
        // 热门文章本地缓存
        hotArticleCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();

        // 文章分页列表本地缓存
        articlePageCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats()
                .build();
    }
}
