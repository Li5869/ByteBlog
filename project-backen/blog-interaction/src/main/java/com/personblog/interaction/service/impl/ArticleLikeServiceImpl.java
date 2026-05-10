package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.ArticleLike;
import com.personblog.interaction.mapper.ArticleLikeMapper;
import com.personblog.interaction.service.ArticleLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.personblog.common.constant.RedisKeys.LIKE_BIZ_KEY_PREFIX;
import static com.personblog.common.constant.TargetTypeConstant.ARTICLE;

/**
 * 文章点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements ArticleLikeService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveLike(Long userId, Long targetId) {
        ArticleLike articleLike = new ArticleLike();
        articleLike.setUserId(userId);
        articleLike.setArticleId(targetId);
        articleLike.setCreatedAt(LocalDateTime.now());
        save(articleLike);
    }

    @Override
    public void removeLike(Long userId, Long targetId) {
        remove(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, targetId));
    }

    @Override
    public Boolean getIsLike(Long userId, Long targetId) {
        return lambdaQuery()
                .eq(ArticleLike::getArticleId,targetId)
                .eq(ArticleLike::getUserId,userId)
                .exists();
    }

    @Override
    public void AllSync2Cache() {
        List<ArticleLike> list = lambdaQuery().select(ArticleLike::getArticleId, ArticleLike::getUserId)
                .list();
        // pipeline优化性能
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (ArticleLike articleLike : list) {
                String key = LIKE_BIZ_KEY_PREFIX(ARTICLE, articleLike.getArticleId());
                src.sAdd(key, articleLike.getUserId().toString());
            }
            return null;
        });
    }
}
