package com.personblog.api.interactionAPI;

import com.personblog.common.dto.Interaction.LikeMessageDTO;

import java.util.List;

public interface CommentApi {
    void updateLikes(List<LikeMessageDTO> dtoList);
    
    Long getCommentAuthorId(Long commentId);

    void updateReviewStatue(Long commentId,String statue);
}
