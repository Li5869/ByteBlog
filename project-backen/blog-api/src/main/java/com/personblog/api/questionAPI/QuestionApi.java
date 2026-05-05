package com.personblog.api.questionAPI;

import com.personblog.common.dto.Interaction.LikeMessageDTO;

import java.util.List;

public interface QuestionApi {
    void updateLikeCount(List<LikeMessageDTO> likeMessageDTOS);
    
    Long getQuestionAuthorId(Long questionId);
    
    String getQuestionTitle(Long questionId);
}
