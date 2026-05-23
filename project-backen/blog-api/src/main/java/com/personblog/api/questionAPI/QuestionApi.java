package com.personblog.api.questionAPI;

import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;

import java.util.List;

/**
 * 问题 API 接口
 * 用于跨模块调用问题服务
 *
 * @author LSH
 */
public interface QuestionApi {

    /**
     * 批量更新问题点赞数
     * 由 MQ 消费者调用，同步 Redis 点赞数到数据库
     *
     * @param dtoList 点赞消息列表
     */
    void updateLikeCount(List<LikeMessage> dtoList);
}
