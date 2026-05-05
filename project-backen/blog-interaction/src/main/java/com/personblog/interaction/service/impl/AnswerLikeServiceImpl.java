package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.AnswerLike;
import com.personblog.interaction.mapper.AnswerLikeMapper;
import com.personblog.interaction.service.AnswerLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 回答点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class AnswerLikeServiceImpl extends ServiceImpl<AnswerLikeMapper, AnswerLike> implements AnswerLikeService {

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
}
