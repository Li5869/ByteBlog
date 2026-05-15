package com.personblog.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.common.enums.BizCodeEnum;
import com.personblog.common.exception.BizException;
import com.personblog.interaction.entity.Conversation;
import com.personblog.interaction.mapper.ConversationMapper;
import com.personblog.interaction.service.IConversationService;
import com.personblog.interaction.vo.BatchDeleteResultVO;
import com.personblog.interaction.vo.ConversationVO;
import com.personblog.interaction.vo.SenderVO;
import com.personblog.push.service.MessagePushService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.personblog.common.enums.BizCodeEnum.NO_POWER;
import static com.personblog.common.enums.BizCodeEnum.PARAMETER_ERROR;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements IConversationService {

    private final ConversationMapper conversationMapper;
    private final UseApi useApi;
    private final MessagePushService messagePushService;
    @Override
    public List<ConversationVO> getConversationList(Long userId) {
        List<Conversation> conversations = conversationMapper.selectConversationsByUserId(userId);

        if (conversations.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> targetUserIds = conversations.stream()
                .map(Conversation::getTargetUserId)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        if (!targetUserIds.isEmpty()) {
            List<UserDTO> users = useApi.getUserInfo(targetUserIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        Map<Long, UserDTO> finalUserMap = userMap;
        return conversations.stream()
                .map(conversation -> {
                    ConversationVO vo = new ConversationVO();
                    vo.setId(conversation.getId());
                    vo.setLastMessage(conversation.getLastMessage());
                    vo.setUnreadCount(conversation.getUnreadCount());
                    vo.setUpdatedAt(conversation.getUpdatedAt());

                    UserDTO targetUser = finalUserMap.get(conversation.getTargetUserId());
                    if (targetUser != null) {
                        vo.setUser(new SenderVO(targetUser.getId(), targetUser.getNickname(), targetUser.getAvatar()));
                    } else {
                        vo.setUser(new SenderVO(conversation.getTargetUserId(), "用户", ""));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteConversation(Long userId, Long id) {
        Conversation conversation = this.getById(id);
        if (conversation == null) {
            throw new BizException(BizCodeEnum.NOT_FOUND_NOTIFICATION);
        }

        if (!conversation.getUserId().equals(userId)) {
            throw new BizException(NO_POWER);
        }

        this.removeById(id);
    }

    @Override
    public BatchDeleteResultVO batchDeleteConversations(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(PARAMETER_ERROR);
        }

        if (ids.size() > 100) {
            throw new BizException(PARAMETER_ERROR);
        }

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Conversation::getId, ids)
                .eq(Conversation::getUserId, userId);

        long count = this.count(wrapper);
        if (count > 0) {
            this.remove(wrapper);
        }

        return new BatchDeleteResultVO((int) count);
    }
    @Resource(name = "MessageExecutor")
    private Executor executor;

    @Override
    public void updateLastMessage(Long senderId, Long receiverId, String content) {
        LocalDateTime now = LocalDateTime.now();
        CompletableFuture.runAsync(() -> {
            updateOrCreateConversation(senderId, receiverId, content, now, false);
            updateOrCreateConversation(receiverId, senderId, content, now, true);
        }, executor);
    }

    private void updateOrCreateConversation(Long userId, Long targetUserId, String content, LocalDateTime time, boolean incrementUnread) {
        Conversation conversation = lambdaQuery()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getTargetUserId, targetUserId)
                .one();
        
        if (conversation != null) {
            conversation.setLastMessage(content);
            conversation.setLastMessageTime(time);
            conversation.setUpdatedAt(time);  // 手动更新 updatedAt
            if (incrementUnread) {
                conversation.setUnreadCount(conversation.getUnreadCount() == null ? 1 : conversation.getUnreadCount() + 1);
                updateById(conversation);
                messagePushService.pushUnreadCountUpdate(userId, 1);
            } else {
                updateById(conversation);
            }
        } else {
            Conversation newConversation = new Conversation();
            newConversation.setUserId(userId);
            newConversation.setTargetUserId(targetUserId);
            newConversation.setLastMessage(content);
            newConversation.setLastMessageTime(time);
            newConversation.setCreatedAt(time);
            newConversation.setUpdatedAt(time);
            newConversation.setUnreadCount(incrementUnread ? 1 : 0);
            save(newConversation);
        }
    }

    @Override
    public void resetUnreadCount(Long userId, Long targetUserId) {
        conversationMapper.resetUnreadCount(userId, targetUserId);
    }
}
