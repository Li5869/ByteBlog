package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.AnswerLike;
import com.personblog.interaction.mapper.AnswerLikeMapper;
import com.personblog.interaction.service.AnswerLikeService;
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
import static com.personblog.common.constant.TargetTypeConstant.ANSWER;

/**
 * 回答点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerLikeServiceImpl extends ServiceImpl<AnswerLikeMapper, AnswerLike> implements AnswerLikeService {
    private final StringRedisTemplate redisTemplate;
    @Override
    public void saveLike(Long userId, Long targetId) {
        AnswerLike answerLike = new AnswerLike();
        answerLike.setUserId(userId);
        answerLike.setAnswerId(targetId);
        answerLike.setCreatedAt(LocalDateTime.now());
        save(answerLike);
    }

    @Override
    public void removeLike(Long userId, Long targetId) {
        remove(new LambdaQueryWrapper<AnswerLike>()
                .eq(AnswerLike::getUserId, userId)
                .eq(AnswerLike::getAnswerId, targetId));
    }

    @Override
    public Boolean getIsLike(Long userId, Long targetId) {
        return lambdaQuery()
                .eq(AnswerLike::getAnswerId,targetId)
                .eq(AnswerLike::getUserId,userId)
                .exists();
    }

    @Override
    public void AllSync2Cache(Long bizId) {
        if (bizId == null) {
            log.warn("AllSync2Cache: bizId 为空，跳过同步");
            return;
        }
        List<AnswerLike> list = lambdaQuery().select(AnswerLike::getAnswerId, AnswerLike::getUserId)
                .eq(AnswerLike::getAnswerId,bizId)
                .list();
        //pipline优化性能
        redisTemplate.executePipelined((RedisCallback<Object>) connection->{
            StringRedisConnection src = new DefaultStringRedisConnection(connection);
            for (AnswerLike answerLike : list) {
                String key = LIKE_BIZ_KEY_PREFIX(ANSWER,answerLike.getAnswerId());
                src.sAdd(key,answerLike.getUserId().toString());
            }
            return null;
        });
    }
}
