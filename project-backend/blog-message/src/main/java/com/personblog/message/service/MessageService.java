package com.personblog.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.personblog.common.dto.Notification.UnreadCountVO;
import com.personblog.message.dto.MessageQueryDTO;
import com.personblog.message.entity.Message;
import com.personblog.message.vo.MessageVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface MessageService extends IService<Message> {

    Page<MessageVO> getMessageHistory(Long currentUserId, Long targetUserId, MessageQueryDTO dto);

    void markAsRead(Long currentUserId, Long senderId);

    UnreadCountVO getUnreadCount(Long userId);

    MessageVO sendMessage(Long senderId, @NotNull(message = "接收者ID不能为空") Long receiverId, @NotBlank(message = "消息内容不能为空") @Size(max = 2000, message = "消息内容不能超过2000字") String content);
}
