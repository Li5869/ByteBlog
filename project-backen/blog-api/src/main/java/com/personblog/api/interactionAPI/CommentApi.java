package com.personblog.api.interactionAPI;

import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;

import java.util.List;

public interface CommentApi {
    void updateLikeCount(List<LikeMessage> dtoList);

    void updateReviewStatue(Long commentId,String statue);

}
