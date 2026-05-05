package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.QuestionLike;
import com.personblog.interaction.mapper.QuestionLikeMapper;
import com.personblog.interaction.service.QuestionLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 问题点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class QuestionLikeServiceImpl extends ServiceImpl<QuestionLikeMapper, QuestionLike> implements QuestionLikeService {

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
}
