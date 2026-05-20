package com.personblog.api.interactionAPI;

import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;

import java.util.List;

/**
 * 评论 API 接口
 * 用于跨模块调用评论服务，处理评论相关操作
 *
 * @author LSH
 */
public interface CommentApi {

    /**
     * 批量更新评论点赞数
     * 由 MQ 消费者调用，同步 Redis 点赞数到数据库
     *
     * @param dtoList 点赞消息列表
     */
    void updateLikeCount(List<LikeMessage> dtoList);

    /**
     * 更新评论审核状态
     *
     * @param commentId 评论ID
     * @param status    审核状态（pending/approved/rejected）
     */
    void updateReviewStatue(Long commentId, String status);
}
