package com.personblog.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.ai.entity.AiMessage;

import java.util.List;

public interface IAiMessageService extends IService<AiMessage> {

    List<AiMessage> selectByConversationId(Long conversationId);
}
