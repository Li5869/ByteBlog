package com.personblog.api.questionAPI;

import com.personblog.common.dto.MqMessage.Interaction.LikeMessage;

import java.util.List;

public interface AnswerApi {
    void updateLikeCount(List<LikeMessage> likeMessageDTOS);
}
