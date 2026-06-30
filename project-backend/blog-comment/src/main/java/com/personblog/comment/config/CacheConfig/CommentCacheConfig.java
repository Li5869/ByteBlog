package com.personblog.comment.config.CacheConfig;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.personblog.comment.vo.CommentVO;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CommentCacheConfig {
    // 本地缓存 - 评论分页
    public static Cache<String, Page<CommentVO>> commentPageCache;

    @PostConstruct
    public  void initCommentCache() {
        commentPageCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats()
                .build();
    }
}
