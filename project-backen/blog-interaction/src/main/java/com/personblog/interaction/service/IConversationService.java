package com.personblog.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.interaction.entity.Conversation;
import com.personblog.interaction.vo.ConversationVO;
import com.personblog.notification.vo.BatchDeleteResultVO;

import java.util.List;

public interface IConversationService extends IService<Conversation> {

    List<ConversationVO> getConversationList(Long userId);

    void deleteConversation(Long userId, Long id);

    BatchDeleteResultVO batchDeleteConversations(Long userId, List<Long> ids);

    void updateLastMessage(Long senderId, Long receiverId, String content);

    void resetUnreadCount(Long userId, Long targetUserId);
}
