package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.interaction.entity.CommentLike;
import com.personblog.interaction.mapper.CommentLikeMapper;
import com.personblog.interaction.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 评论点赞服务实现类
 *
 * @author LSH
 */
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {

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
}
