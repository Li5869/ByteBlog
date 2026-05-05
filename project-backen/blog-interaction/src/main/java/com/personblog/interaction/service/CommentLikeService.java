package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.CommentLike;
import com.personblog.interaction.strategy.LikeStrategy;

/**
 * 评论点赞服务
 *
 * @author LSH
 */
public interface CommentLikeService extends IService<CommentLike>, LikeStrategy {

}
