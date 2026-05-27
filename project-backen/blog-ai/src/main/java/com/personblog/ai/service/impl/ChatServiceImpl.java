package com.personblog.ai.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.personblog.ai.dto.ConversationQueryDTO;
import com.personblog.ai.entity.AiConversation;
import com.personblog.ai.entity.AiMessage;
import com.personblog.ai.service.ChatService;
import com.personblog.ai.service.IAiConversationService;
import com.personblog.ai.service.IAiMessageService;
import com.personblog.ai.vo.ConversationDetailVO;
import com.personblog.ai.vo.ConversationListVO;
import com.personblog.ai.vo.MessageVO;
import com.personblog.common.exception.BizException;
import com.personblog.common.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.personblog.common.constant.RedisKeys.REDIS_MEMORY_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final IAiConversationService conversationService;
    private final IAiMessageService messageService;
    private final StringRedisTemplate redisTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation() {
        Long userId = UserContextHolder.getUserId();
        AiConversation conversion = conversationService.lambdaQuery()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getIsDeleted,false)
                .eq(AiConversation::getMessageCount, 0)
                .one();
        if (conversion != null) {
            return conversion.getId();
        }
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setMessageCount(0);
        conversation.setIsDeleted(false);

        conversationService.save(conversation);

        return conversation.getId();
    }

    @Override
    public Page<ConversationListVO> getConversationList(ConversationQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();

        Page<AiConversation> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getIsDeleted, false)
                .orderByDesc(AiConversation::getCreatedAt);

        Page<AiConversation> result = conversationService.page(page, wrapper);

        Page<ConversationListVO> voPage = new Page<>();
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToListVO)
                .toList());

        return voPage;
    }

    @Override
    public ConversationDetailVO getConversationDetail(Long conversationId) {
        Long userId = UserContextHolder.getUserId();

        AiConversation conversation = conversationService.getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BizException("会话不存在");
        }

        List<MessageVO> messages = getMessagesFromRedisOrDb(conversationId);

        return ConversationDetailVO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .messageCount(conversation.getMessageCount())
                .messages(messages)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private List<MessageVO> getMessagesFromRedisOrDb(Long conversationId) {
        try {
            String threadId = conversationId.toString();
            String key = REDIS_MEMORY_PREFIX + threadId;
            List<String> range = redisTemplate.boundListOps(key).range(0, 999);
            
            if (range != null && !range.isEmpty()) {
                List<MessageVO> list = range.stream()
                        .map(this::parseRedisMessage)
                        .filter(Objects::nonNull)
                        .toList();
                log.info("从 Redis 获取会话 {} 的历史消息，共 {} 条", conversationId, list.size());
                return list;
            }
        } catch (Exception e) {
            log.warn("从 Redis 获取历史消息失败，回退到数据库查询: {}", e.getMessage());
        }

        log.info("从数据库获取会话 {} 的历史消息", conversationId);
        List<AiMessage> dbMessages = messageService.selectByConversationId(conversationId);
        return dbMessages.stream()
                .map(this::convertToMessageVO)
                .toList();
    }

    private MessageVO parseRedisMessage(String jsonStr) {
        try {
            JSONObject map = JSONUtil.parseObj(jsonStr);

            MessageVO.MessageVOBuilder builder = MessageVO.builder()
                    .role(map.getStr("role"))
                    .thinking(map.getStr("thinking"))
                    .content(map.getStr("content"));

            Object createdAt = map.getObj("created_at");
            if (createdAt != null) {
                builder.createdAt(parseDateTime(createdAt.toString()));
            }

            Object toolCallsObj = map.getObj("tool_calls");
            if (toolCallsObj instanceof List<?> toolCallsList) {
                List<MessageVO.ToolCallVO> toolCalls = toolCallsList.stream()
                        .filter(Map.class::isInstance)
                        .map(tc -> (Map<?, ?>) tc)
                        .map(tc -> new MessageVO.ToolCallVO(
                                tc.get("id") != null ? ((Number) tc.get("id")).intValue() : null,
                                (String) tc.get("name"),
                                tc.get("args") != null ? JSONUtil.toJsonStr(tc.get("args")) : null,
                                (String) tc.get("result")
                        ))
                        .toList();
                builder.toolCalls(toolCalls);
            }

            return builder.build();
        } catch (Exception e) {
            log.warn("解析 Redis 消息失败: {}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                return LocalDateTime.parse(dateStr, formatter);
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long conversationId) {
        Long userId = UserContextHolder.getUserId();

        AiConversation conversation = conversationService.getById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BizException("会话不存在");
        }
        String key = REDIS_MEMORY_PREFIX+conversationId;
        redisTemplate.delete(key);
        LambdaUpdateWrapper<AiConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConversation::getId, conversationId)
                .set(AiConversation::getIsDeleted, true);
        conversationService.update(wrapper);
        messageService.remove(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId,conversationId));
    }

    private ConversationListVO convertToListVO(AiConversation conversation) {
        return ConversationListVO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .messageCount(conversation.getMessageCount())
                .lastMessage(conversation.getLastMessage())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageVO convertToMessageVO(AiMessage message) {
        return MessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .thinking(message.getThinking())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
