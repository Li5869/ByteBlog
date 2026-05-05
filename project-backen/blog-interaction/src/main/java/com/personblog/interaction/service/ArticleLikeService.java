package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.ArticleLike;
import com.personblog.interaction.strategy.LikeStrategy;

/**
 * 文章点赞服务
 *
 * @author LSH
 */
public interface ArticleLikeService extends IService<ArticleLike>, LikeStrategy {

}
