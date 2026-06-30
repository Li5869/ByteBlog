package com.personblog.message.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.personblog.api.usrAPI.UseApi;
import com.personblog.common.dto.Notification.UnreadCountVO;
import com.personblog.common.dto.User.UserDTO;
import com.personblog.message.dto.MessageQueryDTO;
import com.personblog.message.entity.Message;
import com.personblog.message.mapper.MessageMapper;
import com.personblog.message.service.IConversationService;
import com.personblog.message.service.MessageService;
import com.personblog.message.vo.MessageVO;
import com.personblog.push.service.MessagePushService;
import com.personblog.push.vo.PushMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageMapper messageMapper;
    private final IConversationService conversationService;
    private final MessagePushService messagePushService;
    private final UseApi useApi;

    @Override
    public Page<MessageVO> getMessageHistory(Long currentUserId, Long targetUserId, MessageQueryDTO dto) {
        int current = dto.getCurrent() == null ? 1 : dto.getCurrent();
        int size = dto.getSize() == null ? 20 : Math.min(dto.getSize(), 100);

        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(Message::getSenderId, currentUserId).eq(Message::getReceiverId, targetUserId)
                .or()
                .eq(Message::getSenderId, targetUserId).eq(Message::getReceiverId, currentUserId)
        );
        wrapper.orderByDesc(Message::getCreatedAt);

        Page<Message> messagePage = this.page(page, wrapper);

        List<Message> messages = messagePage.getRecords();
        if (messages.isEmpty()) {
            Page<MessageVO> voPage = new Page<>(current, size, messagePage.getTotal());
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<MessageVO> voList = messages.stream()
                .map(message -> {
                    MessageVO vo = BeanUtil.copyProperties(message, MessageVO.class);
                    if (message.getSenderId().equals(currentUserId)) {
                        vo.setType("sent");
                    } else {
                        vo.setType("received");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        Page<MessageVO> voPage = new Page<>(current, size, messagePage.getTotal());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public void markAsRead(Long currentUserId, Long senderId) {
        messageMapper.markAsReadBySender(currentUserId, senderId);
        conversationService.resetUnreadCount(currentUserId, senderId);
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, false);

        long count = this.count(wrapper);
        return new UnreadCountVO((int) count);
    }

    @Override
    public MessageVO sendMessage(Long senderId, Long receiverId, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        save(message);
        conversationService.updateLastMessage(senderId, receiverId, content);
        MessageVO vo = BeanUtil.copyProperties(message, MessageVO.class);
        List<UserDTO> userInfo = useApi.getUserInfo(List.of(receiverId));
        UserDTO userDTO = userInfo != null ? userInfo.getFirst() : null;
        if (userDTO != null) {
            PushMessageVO messageVO = new PushMessageVO();
            messageVO.setMessageId(message.getId());
            messageVO.setSenderAvatar(userDTO.getAvatar());
            messageVO.setSenderName(userDTO.getNickname());
            messageVO.setContent(content);
            messageVO.setSenderId(senderId);
            messageVO.setReceiverId(receiverId);
            messageVO.setCreatedAt(message.getCreatedAt());
            messagePushService.pushMessage(messageVO);
        }

        return vo;
    }
}
