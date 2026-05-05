package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.ArticleLike;
import com.personblog.interaction.mapper.ArticleLikeMapper;
import com.personblog.interaction.service.ArticleLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 文章点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike> implements ArticleLikeService {

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
}
