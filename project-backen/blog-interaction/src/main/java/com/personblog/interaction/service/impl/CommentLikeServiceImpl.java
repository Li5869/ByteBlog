package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.CommentLike;
import com.personblog.interaction.mapper.CommentLikeMapper;
import com.personblog.interaction.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.personblog.common.constant.RedisKeys.LIKE_BIZ_KEY_PREFIX;
import static com.personblog.common.constant.TargetTypeConstant.COMMENT;

/**
 * 评论点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveLike(Long userId, Long targetId) {
        CommentLike commentLike = new CommentLike();
        commentLike.setUserId(userId);
        commentLike.setCommentId(targetId);
        commentLike.setCreatedAt(LocalDateTime.now());
        save(commentLike);
    }

    @Override
    public void removeLike(Long userId, Long targetId) {
        remove(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getUserId, userId)
                .eq(CommentLike::getCommentId, targetId));
    }

    @Override
    public Boolean getIsLike(Long userId, Long targetId) {
        return lambdaQuery()
                .eq(CommentLike::getCommentId, targetId)
                .eq(CommentLike::getUserId, userId)
                .exists();
    }

    @Override
    public void AllSync2Cache(Long bizId) {
        if (bizId == null) {
            log.warn("AllSync2Cache: bizId 为空，跳过同步");
            return;
        }
        List<CommentLike> list = lambdaQuery().select(CommentLike::getCommentId, CommentLike::getUserId)
                .eq(CommentLike::getCommentId,bizId)
                .list();
        // pipeline优化性能
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (CommentLike commentLike : list) {
                String key = LIKE_BIZ_KEY_PREFIX(COMMENT, commentLike.getCommentId());
                src.sAdd(key, commentLike.getUserId().toString());
            }
            return null;
        });
    }
}
