package com.personblog.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.dto.Notification.BatchDeleteResultVO;
import com.personblog.message.entity.Conversation;
import com.personblog.message.vo.ConversationVO;

import java.util.List;

public interface IConversationService extends IService<Conversation> {

    List<ConversationVO> getConversationList(Long userId);

    void deleteConversation(Long userId, Long id);

    BatchDeleteResultVO batchDeleteConversations(Long userId, List<Long> ids);

    void updateLastMessage(Long senderId, Long receiverId, String content);

    void resetUnreadCount(Long userId, Long targetUserId);
}
