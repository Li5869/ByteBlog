package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.QuestionLike;
import com.personblog.interaction.strategy.LikeStrategy;

/**
 * 问题点赞服务
 *
 * @author LSH
 */
public interface QuestionLikeService extends IService<QuestionLike>, LikeStrategy {

}
