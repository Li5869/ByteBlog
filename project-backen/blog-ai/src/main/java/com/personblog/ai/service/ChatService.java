package com.personblog.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.dto.ConversationQueryDTO;
import com.personblog.ai.vo.ConversationDetailVO;
import com.personblog.ai.vo.ConversationListVO;

public interface ChatService{

    Long createConversation();

    Page<ConversationListVO> getConversationList(ConversationQueryDTO dto);

    ConversationDetailVO getConversationDetail(Long conversationId);

    void deleteConversation(Long conversationId);
}
