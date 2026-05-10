package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.QuestionLike;
import com.personblog.interaction.mapper.QuestionLikeMapper;
import com.personblog.interaction.service.QuestionLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.personblog.common.constant.RedisKeys.LIKE_BIZ_KEY_PREFIX;
import static com.personblog.common.constant.TargetTypeConstant.QUESTION;

/**
 * 问题点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class QuestionLikeServiceImpl extends ServiceImpl<QuestionLikeMapper, QuestionLike> implements QuestionLikeService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void saveLike(Long userId, Long targetId) {
        QuestionLike questionLike = new QuestionLike();
        questionLike.setUserId(userId);
        questionLike.setQuestionId(targetId);
        questionLike.setCreatedAt(LocalDateTime.now());
        save(questionLike);
    }

    @Override
    public void removeLike(Long userId, Long targetId) {
        remove(new LambdaQueryWrapper<QuestionLike>()
                .eq(QuestionLike::getUserId, userId)
                .eq(QuestionLike::getQuestionId, targetId));
    }

    @Override
    public Boolean getIsLike(Long userId, Long targetId) {
        return lambdaQuery()
                .eq(QuestionLike::getQuestionId, targetId)
                .eq(QuestionLike::getUserId, userId)
                .exists();
    }

    @Override
    public void AllSync2Cache() {
        List<QuestionLike> list = lambdaQuery().select(QuestionLike::getQuestionId, QuestionLike::getUserId)
                .list();
        // pipeline优化性能
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (QuestionLike questionLike : list) {
                String key = LIKE_BIZ_KEY_PREFIX(QUESTION, questionLike.getQuestionId());
                src.sAdd(key, questionLike.getUserId().toString());
            }
            return null;
        });
    }
}
