package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.ai.entity.AiMessage;
import com.personblog.ai.mapper.AiMessageMapper;
import com.personblog.ai.service.IAiMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements IAiMessageService {

    @Override
    public List<AiMessage> selectByConversationId(Long conversationId) {
        return baseMapper.selectByConversationId(conversationId);
    }
}
