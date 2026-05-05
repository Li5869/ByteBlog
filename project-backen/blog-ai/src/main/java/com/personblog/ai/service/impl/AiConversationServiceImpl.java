package com.personblog.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.ai.entity.AiConversation;
import com.personblog.ai.mapper.AiConversationMapper;
import com.personblog.ai.service.IAiConversationService;
import org.springframework.stereotype.Service;

@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation> implements IAiConversationService {
}
