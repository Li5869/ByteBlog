package com.personblog.api.questionAPI;

import com.personblog.common.dto.Interaction.LikeMessageDTO;

import java.util.List;

public interface AnswerApi {
    void updateLikeCount(List<LikeMessageDTO> likeMessageDTOS);
    
    Long getAnswerAuthorId(Long answerId);
}
