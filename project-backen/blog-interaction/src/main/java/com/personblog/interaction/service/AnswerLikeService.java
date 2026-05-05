package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.AnswerLike;
import com.personblog.interaction.strategy.LikeStrategy;

/**
 * 回答点赞服务
 *
 * @author LSH
 */
public interface AnswerLikeService extends IService<AnswerLike>, LikeStrategy {

}
